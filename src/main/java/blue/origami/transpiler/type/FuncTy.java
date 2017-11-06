package blue.origami.transpiler.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import blue.origami.common.OArrays;
import blue.origami.common.ODebug;
import blue.origami.common.OStrings;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.code.ApplyCode;
import blue.origami.transpiler.code.CastCode;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.FuncCode;
import blue.origami.transpiler.code.VarNameCode;

public class FuncTy extends Ty {
	protected final Ty[] paramTypes;
	protected final Ty returnType;

	FuncTy(Ty returnType, Ty... paramTypes) {
		this.paramTypes = paramTypes;
		this.returnType = returnType;
	}

	public Ty getReturnType() {
		return this.returnType;
	}

	public int getParamSize() {
		return this.paramTypes.length;
	}

	public Ty[] getParamTypes() {
		return this.paramTypes;
	}

	public final static void stringfy(StringBuilder sb, Ty[] paramTypes, Ty returnType, Consumer<Ty> f) {
		if (paramTypes.length != 1 || paramTypes[0].isFunc() /* group */) {
			OStrings.forEach(sb, paramTypes.length, "(", ",", ")", (n) -> {
				f.accept(paramTypes[n]);
			});
		} else {
			f.accept(paramTypes[0]);
		}
		sb.append("->");
		OStrings.enclosed(sb, returnType.isFunc(), "(", ")", () -> f.accept(returnType));
	}

	@Override
	public void strOut(StringBuilder sb) {
		stringfy(sb, this.paramTypes, this.returnType, t -> t.strOut(sb));
	}

	@Override
	public void typeKey(StringBuilder sb) {
		sb.append("(");
		stringfy(sb, this.paramTypes, this.returnType, t -> t.typeKey(sb));
		sb.append(")");
	}

	@Override
	public String keyFrom() {
		return "@";
	}

	@Override
	public boolean hasSome(Predicate<Ty> f) {
		return this.returnType.hasSome(f) || OArrays.testSome(this.getParamTypes(), t -> t.hasSome(f));
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		if (this.hasSome(Ty.IsVarParam)) {
			Ty[] p = Ty.map(this.paramTypes, x -> x.dupVar(dom));
			Ty ret = this.returnType.dupVar(dom);
			return Ty.tFunc(ret, p);
		}
		return this;
	}

	@Override
	public Ty map(Function<Ty, Ty> f) {
		Ty self = f.apply(this);
		if (self != this) {
			return self;
		}
		Ty r = this.returnType.map(f);
		Ty[] ts = Ty.map(this.paramTypes, x -> x.map(f));
		if (r == this.returnType && Arrays.equals(ts, this.paramTypes)) {
			return this;
		}
		return Ty.tFunc(r, ts);
	}

	@Override
	public boolean match(boolean sub, Ty codeTy, TypeMatcher logs) {
		if (codeTy.isFunc()) {
			FuncTy funcTy = (FuncTy) codeTy.base();
			if (funcTy.getParamSize() != this.getParamSize()) {
				return false;
			}
			for (int i = 0; i < this.getParamSize(); i++) {
				if (!this.paramTypes[i].match(false, funcTy.paramTypes[i], logs)) {
					return false;
				}
			}
			return this.returnType.match(false, funcTy.returnType, logs);
		}
		return this.matchVar(sub, codeTy, logs);
	}

	@Override
	public Ty memoed() {
		if (!this.isMemoed()) {
			return Ty.tFunc(this.returnType.memoed(), Ty.map(this.paramTypes, t -> t.memoed()));
		}
		return this;
	}

	@Override
	public <C> C mapType(TypeMapper<C> codeType) {
		return codeType.forFuncType(this);
	}

	@Override
	public int costMapThisTo(Env env, Ty a, Ty ty) {
		assert (this == a);
		if (ty.isFunc()) {
			FuncTy toTy = (FuncTy) ty.base();
			if (this.getParamSize() == toTy.getParamSize()) {
				TypeMatcher logger = new TypeMatcher();
				Ty[] fromTys = this.getParamTypes();
				Ty[] toTys = toTy.getParamTypes();
				int cost = 0;
				for (int i = 0; i < fromTys.length; i++) {
					cost = Math.max(cost, env.arrowCost(env, toTys[i], fromTys[i], logger));
					if (cost >= CodeMap.STUPID) {
						logger.abort();
						return CodeMap.STUPID;
					}
				}
				cost = Math.max(cost, env.arrowCost(env, this.getReturnType(), toTy.getReturnType(), logger));
				logger.abort();
				return cost;
			}
		}
		return CodeMap.STUPID;
	}

	@Override
	public CodeMap findMapThisTo(Env env, Ty a, Ty ty) {
		assert (this == a);
		if (ty.isFunc()) {
			FuncTy toTy = (FuncTy) ty.base();
			int cost = this.costMapThisTo(env, a, ty);
			if (cost < CodeMap.STUPID) {
				return this.genFuncConv(env, this, toTy).setMapCost(cost);
			}
		}
		return null;
	}

	static int seq = 1000;

	public CodeMap genFuncConv(Env env, FuncTy fromTy, FuncTy toTy) {
		ODebug.trace("generating funcmap %s => %s", fromTy, toTy);
		// System.out.println("::::::: genFuncConv " + fromTy + " => " + toTy);
		Transpiler tr = env.getTranspiler();
		AST[] names = AST.getNames("f");
		Ty[] params = { fromTy };

		Ty[] fromTypes = fromTy.getParamTypes();
		Ty[] toTypes = toTy.getParamTypes();
		AST[] fnames = AST.getNames(OArrays.names(toTypes.length));
		List<Code> l = new ArrayList<>();
		l.add(new VarNameCode("f"));
		for (int c = 0; c < toTy.getParamSize(); c++) {
			Code p = new VarNameCode(String.valueOf((char) ('a' + c)));
			l.add(new CastCode(fromTypes[c], p));
			ODebug.trace("[%d] casting %s to %s", c, toTypes[c], fromTypes[c]);
		}
		ODebug.trace("[ret] casting %s to %s", fromTy.getReturnType(), toTy.getReturnType());
		Code body = new CastCode(toTy.getReturnType(), new ApplyCode(l));
		FuncCode func = new FuncCode(fnames, toTypes, toTy.getReturnType(), body);
		return tr.defineFunction2(false, Ty.mapKey2(fromTy, toTy), "funcConv" + (seq++), names, params, toTy, func);
	}

	public CodeMap genFuncConv2(Env env, FuncTy fromTy, FuncTy toTy) {
		ODebug.trace("generating funcmap %s => %s", fromTy, toTy);
		// System.out.println("::::::: genFuncConv " + fromTy + " => " + toTy);
		Transpiler tr = env.getTranspiler();
		AST[] names = AST.getNames("f");
		Ty[] params = { fromTy };

		Ty[] fromTypes = fromTy.getParamTypes();
		Ty[] toTypes = toTy.getParamTypes();
		AST[] fnames = AST.getNames(OArrays.names(toTypes.length));
		List<Code> l = new ArrayList<>();
		l.add(new VarNameCode(names[0], 0, fromTy, 1));
		for (int c = 0; c < toTy.getParamSize(); c++) {
			Code p = new VarNameCode(fnames[c], c, toTypes[c], 0);
			l.add(p.asType(env, fromTypes[c]));
			ODebug.trace("[%d] casting %s to %s", c, toTypes[c], fromTypes[c]);
		}
		ODebug.trace("[ret] casting %s to %s", fromTy.getReturnType(), toTy.getReturnType());
		Code body = new ApplyCode(l).asType(env, toTy.getReturnType());
		FuncCode func = new FuncCode(fnames, toTypes, toTy.getReturnType(), body);
		func.setType(Ty.tFunc(toTy, fromTy));
		return tr.defineFunction2(false, Ty.mapKey2(fromTy, toTy), "funcConv" + (seq++), names, params, toTy, func);
	}

}
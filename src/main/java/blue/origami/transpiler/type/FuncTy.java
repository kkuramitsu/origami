package blue.origami.transpiler.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.code.ApplyCode;
import blue.origami.transpiler.code.CastCode;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.FuncCode;
import blue.origami.transpiler.code.NameCode;
import blue.origami.util.ODebug;
import blue.origami.util.StringCombinator;

public class FuncTy extends Ty {
	protected final String name;
	protected final Ty[] paramTypes;
	protected final Ty returnType;

	FuncTy(String name, Ty returnType, Ty... paramTypes) {
		this.name = name;
		this.paramTypes = paramTypes;
		this.returnType = returnType;
	}

	FuncTy(Ty returnType, Ty... paramTypes) {
		this(null, returnType, paramTypes);
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

	public static String stringfy(Ty returnType, Ty... paramTypes) {
		StringBuilder sb = new StringBuilder();
		stringfy(sb, returnType, paramTypes);
		return sb.toString();
	}

	private static Object cap(Ty ty) {
		return (ty instanceof FuncTy) ? "(" + ty + ")" : ty;
	}

	static void stringfy(StringBuilder sb, Ty returnType, Ty... paramTypes) {
		if (paramTypes.length != 1) {
			sb.append("(");
			StringCombinator.joins(sb, paramTypes, ",", (ty) -> cap(ty));
			sb.append(")");
		} else {
			StringCombinator.joins(sb, paramTypes, ",", (ty) -> cap(ty));
		}
		sb.append("->");
		sb.append(returnType);
	}

	@Override
	public void strOut(StringBuilder sb) {
		if (this.name != null) {
			sb.append(this.name);
		} else {
			stringfy(sb, this.getReturnType(), this.getParamTypes());
		}
	}

	@Override
	public String key() {
		return this.toString();
	}

	@Override
	public boolean hasVar() {
		return this.returnType.hasVar() || Ty.hasVar(this.getParamTypes());
	}

	@Override
	public Ty dupVarType(VarDomain dom) {
		if (this.name == null && this.hasVar()) {
			return Ty.tFunc(this.returnType.dupVarType(dom),
					Arrays.stream(this.paramTypes).map(x -> x.dupVarType(dom)).toArray(Ty[]::new));
		}
		return this;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		if (codeTy instanceof VarTy) {
			return (codeTy.acceptTy(false, this, logs));
		}
		if (codeTy instanceof FuncTy) {
			FuncTy ft = (FuncTy) codeTy;
			if (ft.getParamSize() != this.getParamSize()) {
				return false;
			}
			for (int i = 0; i < this.getParamSize(); i++) {
				if (!this.paramTypes[i].acceptTy(false, ft.paramTypes[i], logs)) {
					return false;
				}
			}
			return this.returnType.acceptTy(false, ft.returnType, logs);
		}
		return false;
	}

	@Override
	public boolean isDynamic() {
		return Ty.isDynamic(this.getParamTypes()) || this.returnType.isDynamic();
	}

	@Override
	public Ty staticTy() {
		if (this.name == null) {
			Ty[] p = Arrays.stream(this.paramTypes).map(x -> x.staticTy()).toArray(Ty[]::new);
			Ty ret = this.returnType.staticTy();
			return Ty.tFunc(ret, p);
		}
		return this;
	}

	@Override
	public <C> C mapType(TypeMap<C> codeType) {
		return codeType.mapType(this);
	}

	@Override
	public int costMap(TEnv env, Ty ty) {
		if (ty instanceof FuncTy) {
			FuncTy toTy = (FuncTy) ty;
			if (this.getParamSize() == toTy.getParamSize()) {
				VarLogger logger = new VarLogger();
				Ty[] fromTys = this.getParamTypes();
				Ty[] toTys = toTy.getParamTypes();
				int cost = 0;
				for (int i = 0; i < fromTys.length; i++) {
					cost = Math.max(cost, env.mapCost(env, toTys[i], fromTys[i], logger));
					if (cost >= CastCode.STUPID) {
						logger.abort();
						return CastCode.STUPID;
					}
				}
				cost = Math.max(cost, env.mapCost(env, this.getReturnType(), toTy.getReturnType(), logger));
				logger.abort();
				return cost;
			}
		}
		return CastCode.STUPID;
	}

	@Override
	public Template findMap(TEnv env, Ty ty) {
		if (ty instanceof FuncTy) {
			FuncTy toTy = (FuncTy) ty;
			int cost = this.costMap(env, ty);
			if (cost < CastCode.STUPID) {
				return this.genFuncConv(env, this, toTy).setMapCost(cost);
			}
		}
		return null;
	}

	public static String mapKey(Ty fromTy, Ty toTy) {
		StringBuilder sb = new StringBuilder();
		if (fromTy.isFunc()) {
			sb.append("(");
			fromTy.strOut(sb);
			sb.append(")");
		} else {
			fromTy.strOut(sb);
		}
		sb.append("->");
		if (toTy.isFunc()) {
			sb.append("(");
			toTy.strOut(sb);
			sb.append(")");
		} else {
			toTy.strOut(sb);
		}
		return sb.toString();
	}

	public Template genFuncConv(TEnv env, FuncTy fromTy, FuncTy toTy) {
		ODebug.trace("generating funcmap %s => %s", fromTy, toTy);
		Transpiler tr = env.getTranspiler();
		String[] names = { "_f" };
		Ty[] params = { fromTy };

		Ty[] fromTypes = fromTy.getParamTypes();
		Ty[] toTypes = toTy.getParamTypes();
		String[] fnames = TArrays.names(toTypes.length);
		List<Code> l = new ArrayList<>();
		l.add(new NameCode("_f"));
		for (int c = 0; c < toTy.getParamSize(); c++) {
			l.add(new CastCode(fromTypes[c], new NameCode(String.valueOf((char) c))));
		}
		Code body = new CastCode(toTy.getReturnType(), new ApplyCode(l));
		body = new FuncCode(fnames, toTypes, toTy.getReturnType(), body);
		return tr.defineFunction(mapKey(fromTy, toTy), names, params, toTy, body);
	}

}
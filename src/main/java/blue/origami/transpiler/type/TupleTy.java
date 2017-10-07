package blue.origami.transpiler.type;

import java.util.Arrays;

import blue.origami.transpiler.TArrays;
import blue.origami.util.OStrings;

public class TupleTy extends Ty {
	private String memoName;
	protected final Ty[] paramTypes;

	public TupleTy(String memoName, Ty... paramTypes) {
		this.memoName = memoName;
		this.paramTypes = paramTypes;
		assert (this.paramTypes.length > 1) : "tuple size " + this.paramTypes.length;
	}

	@Override
	public boolean isNonMemo() {
		return this.memoName == null;
	}

	public int getParamSize() {
		return this.paramTypes.length;
	}

	public Ty[] getParamTypes() {
		return this.paramTypes;
	}

	public static String stringfy(Ty... paramTypes) {
		StringBuilder sb = new StringBuilder();
		stringfy(sb, paramTypes);
		return sb.toString();
	}

	// private static String group(Ty ty) {
	// return (ty.isFunc() || ty.isUnion()) ? "(" + ty + ")" : ty.toString();
	// }

	static void stringfy(StringBuilder sb, Ty... paramTypes) {
		// sb.append("(");
		OStrings.joins(sb, paramTypes, "*");
		// sb.append(")");
	}

	@Override
	public void strOut(StringBuilder sb) {
		if (this.memoName != null) {
			sb.append(this.memoName);
		} else {
			stringfy(sb, this.getParamTypes());
		}
	}

	@Override
	public boolean hasVar() {
		return TArrays.testSomeTrue(t -> t.hasVar(), this.getParamTypes());
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		if (this.hasVar()) {
			return Ty.tTuple(Arrays.stream(this.paramTypes).map(x -> x.dupVar(dom)).toArray(Ty[]::new));
		}
		return this;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		if (codeTy.isTuple()) {
			TupleTy tupleTy = (TupleTy) codeTy.real();
			if (tupleTy.getParamSize() != this.getParamSize()) {
				return false;
			}
			for (int i = 0; i < this.getParamSize(); i++) {
				if (!this.paramTypes[i].acceptTy(false, tupleTy.paramTypes[i], logs)) {
					return false;
				}
			}
			return true;
		}
		return this.acceptVarTy(sub, codeTy, logs);
	}

	@Override
	public Ty finalTy() {
		if (this.memoName != null) {
			Ty[] p = Arrays.stream(this.paramTypes).map(x -> x.finalTy()).toArray(Ty[]::new);
			return Ty.tTuple(p);
		}
		return this;
	}

	@Override
	public <C> C mapType(TypeMap<C> codeType) {
		return codeType.forTupleType(this);
	}

	// @Override
	// public int costMapTo(TEnv env, Ty ty) {
	// if (ty.isFunc()) {
	// TupleTy toTy = (TupleTy) ty.real();
	// if (this.getParamSize() == toTy.getParamSize()) {
	// VarLogger logger = new VarLogger();
	// Ty[] fromTys = this.getParamTypes();
	// Ty[] toTys = toTy.getParamTypes();
	// int cost = 0;
	// for (int i = 0; i < fromTys.length; i++) {
	// cost = Math.max(cost, env.mapCost(env, toTys[i], fromTys[i], logger));
	// if (cost >= CastCode.STUPID) {
	// logger.abort();
	// return CastCode.STUPID;
	// }
	// }
	// cost = Math.max(cost, env.mapCost(env, this.getReturnType(),
	// toTy.getReturnType(), logger));
	// logger.abort();
	// return cost;
	// }
	// }
	// return CastCode.STUPID;
	// }
	//
	// @Override
	// public Template findMapTo(TEnv env, Ty ty) {
	// if (ty.isFunc()) {
	// TupleTy toTy = (TupleTy) ty.real();
	// int cost = this.costMapTo(env, ty);
	// if (cost < CastCode.STUPID) {
	// return this.genFuncConv(env, this, toTy).setMapCost(cost);
	// }
	// }
	// return null;
	// }
	//
	// public static String mapKey(Ty fromTy, Ty toTy) {
	// StringBuilder sb = new StringBuilder();
	// if (fromTy.isFunc()) {
	// sb.append("(");
	// fromTy.strOut(sb);
	// sb.append(")");
	// } else {
	// fromTy.strOut(sb);
	// }
	// sb.append("->");
	// if (toTy.isFunc()) {
	// sb.append("(");
	// toTy.strOut(sb);
	// sb.append(")");
	// } else {
	// toTy.strOut(sb);
	// }
	// return sb.toString();
	// }
	//
	// public Template genFuncConv(TEnv env, TupleTy fromTy, TupleTy toTy) {
	// ODebug.stackTrace("generating funcmap %s => %s", fromTy, toTy);
	// Transpiler tr = env.getTranspiler();
	// String[] names = { "f" };
	// Ty[] params = { fromTy };
	//
	// Ty[] fromTypes = fromTy.getParamTypes();
	// Ty[] toTypes = toTy.getParamTypes();
	// String[] fnames = TArrays.names(toTypes.length);
	// List<Code> l = new ArrayList<>();
	// l.add(new NameCode("f"));
	// for (int c = 0; c < toTy.getParamSize(); c++) {
	// Code p = new NameCode(String.valueOf((char) ('a' + c)));
	// l.add(new CastCode(fromTypes[c], p)); // Any->Int & (Int->Int? &
	// // Int?->Any?) ==> c->d
	// // ODebug.trace("[%d] %s->%s %s", c, toTypes[c], fromTypes[c],
	// // env.findTypeMap(env, toTypes[c], fromTypes[c]));
	// }
	// // ODebug.trace("[ret] %s->%s", fromTy.getReturnType(),
	// // toTy.getReturnType());
	// Code body = new CastCode(toTy.getReturnType(), new ApplyCode(l));
	// FuncCode func = new FuncCode(fnames, toTypes, toTy.getReturnType(),
	// body);
	// return tr.defineFunction(mapKey(fromTy, toTy), names, params, toTy,
	// func);
	// }

}

package blue.origami.transpiler;

import java.util.Arrays;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarDomain;
import blue.origami.util.ODebug;

public interface FunctionUnit {

	public Tree<?> getSource();

	public String getName();

	public String[] getParamNames();

	public Ty[] getParamTypes();

	public Ty getReturnType();

	public void setParamTypes(Ty[] pats);

	public void setReturnType(Ty ret);

	public default Code typeBody(TEnv env, FunctionContext fcx, Tree<?> body) {
		return typeBody(env, fcx, env.parseCode(env, body));
	}

	public default Code typeBody(TEnv env0, FunctionContext fcx, Code code0) {
		String[] pnames = this.getParamNames();
		VarDomain dom = new VarDomain(pnames);
		this.setParamTypes(dom.paramTypes(pnames, this.getParamTypes()));
		this.setReturnType(dom.retType(this.getReturnType()));
		//
		return this.typeCheck(env0, fcx, dom, code0);
	}

	public default Code typeCheck(final TEnv env0, FunctionContext fcx, VarDomain dom, Code code0) {
		final String[] pnames = this.getParamNames();
		final Ty[] pats = this.getParamTypes();
		final Ty ret = this.getReturnType();
		boolean dyn = ret.hasVar();

		final TEnv env = env0.newEnv();
		env.add(FunctionContext.class, fcx);
		fcx.enter();
		for (int i = 0; i < pnames.length; i++) {
			env.add(pnames[i], fcx.newVariable(pnames[i], pats[i]));
		}
		Code code = env.catchCode(() -> code0.asType(env, ret));
		fcx.exit();
		if (dyn) {
			ret.toImmutable();
		}
		if (dom != null) {
			dom.reset();
			this.setParamTypes(Arrays.stream(this.getParamTypes()).map(ty -> {
				boolean hasMutation = ty.hasMutation();
				ty = ty.dupVar(dom);
				ty = ty.finalTy();
				if (!hasMutation || ty.isMutable()) {
					ODebug.trace("FIXME maybe immutable %s", ty);
					ty = ty.toImmutable();
				}
				return ty;
			}).toArray(Ty[]::new));
			int vars = dom.usedVars();
			this.setReturnType(dom.dupRetType(this.getReturnType()).finalTy());
			if (vars == 0 && dom.usedVars() > vars) {
				return new ErrorCode(this.getSource(), TFmt.ambiguous_type__S, this.getReturnType());
			}
		}
		return code;
	}

	// public static Code typeCheck(final TEnv env, FunctionContext fcx,
	// String[] pnames, Ty[] pats, final Ty ret,
	// Code code0) {
	// boolean dyn = ret.hasVar();
	// env.add(FunctionContext.class, fcx);
	// fcx.enter();
	// for (int i = 0; i < pnames.length; i++) {
	// env.add(pnames[i], fcx.newVariable(pnames[i], pats[i]));
	// }
	// Code code = env.catchCode(() -> code0.asType(env, ret));
	// fcx.exit();
	// if (dyn) {
	// ret.toImmutable();
	// }
	// return code;
	// }

	public default boolean isAbstract(Code code) {
		return code.hasSome(c -> c.isAbstract());
	}

	public default boolean hasVarParams(Code code) {
		return TArrays.testSomeTrue(t -> t.hasVar(), this.getParamTypes());
	}

	public default boolean isError(Code code) {
		return code.hasSome(c -> c.isError());
	}

	public static FunctionUnit wrap(Tree<?> s, String[] paramNames, Template tp) {
		return new FunctionUnitWrapper(s, paramNames, tp);
	}

}

class FunctionUnitWrapper implements FunctionUnit {
	Tree<?> at;
	String[] paramNames;
	Template tp;

	public FunctionUnitWrapper(Tree<?> s, String[] paramNames, Template tp) {
		this.at = s;
		this.tp = tp;
		this.paramNames = paramNames;
	}

	@Override
	public Tree<?> getSource() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		return this.tp.getName();
	}

	@Override
	public String[] getParamNames() {
		return this.paramNames;
	}

	@Override
	public Ty[] getParamTypes() {
		return this.tp.getParamTypes();
	}

	@Override
	public Ty getReturnType() {
		return this.tp.getReturnType();
	}

	@Override
	public void setParamTypes(Ty[] pats) {
		this.tp.setParamTypes(pats);
	}

	@Override
	public void setReturnType(Ty ret) {
		this.tp.setReturnType(ret);
	}

}
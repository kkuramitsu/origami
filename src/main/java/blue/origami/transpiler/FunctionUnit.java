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
		Ty[] pats = this.getParamTypes();
		final Ty ret = this.getReturnType();
		final TEnv env = env0.newEnv();
		// String name = this.getName();
		// if (name != null) {
		// ODebug.trace("::::::::::::: name=%s", name);
		// Transpiler tr = env0.getTranspiler();
		// CodeTemplate tp = tr.newTemplate(name, ret, pats);
		// ODebug.trace("name=%s, %s", name, tp);
		// tr.add(name, tp);
		// }
		Code code = typeCheck(env, fcx, pnames, pats, ret, code0);
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
		return code;
	}

	public static Code typeCheck(final TEnv env, FunctionContext fcx, String[] pnames, Ty[] pats, final Ty ret,
			Code code0) {
		boolean dyn = ret.hasVar();
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
		return code;
	}

	public default boolean isAbstract(Code code) {
		return code.hasSome(c -> c.isAbstract());
	}

	public default boolean hasVarParams(Code code) {
		return TArrays.testSomeTrue(t -> t.hasVar(), this.getParamTypes());
	}

	public default boolean isError(Code code) {
		return code.hasSome(c -> c.isError());
	}

}
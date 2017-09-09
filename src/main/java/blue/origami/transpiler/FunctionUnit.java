package blue.origami.transpiler;

import java.util.Arrays;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarDomain;

public interface FunctionUnit {
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
		boolean dyn = ret.hasVar();
		final TEnv env = env0.newEnv();
		// final CodeTemplate tp = this.generator.newFuncTemplate(this, name,
		// name, ret, pats);
		// env.add(name, tp);
		env.add(FunctionContext.class, fcx);
		fcx.enter();
		for (int i = 0; i < pnames.length; i++) {
			env.add(pnames[i], fcx.newVariable(pnames[i], pats[i]));
		}
		// Code code0 = env.parseCode(env, body);
		Code code = env.catchCode(() -> code0.asType(env, ret));
		fcx.exit();
		if (dyn) {
			ret.toImmutable();
		}
		dom.reset();
		this.setParamTypes(
				Arrays.stream(dom.dupParamTypes(this.getParamTypes())).map(t -> t.finalTy()).toArray(Ty[]::new));
		this.setReturnType(dom.dupRetType(this.getReturnType()).finalTy());
		return code;
	}

	public default boolean isAbstract(Code code) {
		return code.hasSome(c -> c.isAbstract());
	}

	public default boolean isError(Code code) {
		return code.hasSome(c -> c.isError());
	}

}
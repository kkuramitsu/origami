package blue.origami.transpiler;

import java.util.Arrays;
import java.util.HashMap;

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

	public void setStartIndex(int startIndex);

	public void setFieldMap(HashMap<String, Code> fieldMap);

	public default Code typeBody(TEnv env, Tree<?> body) {
		return typeBody(env, env.parseCode(env, body));
	}

	public default Code typeBody(TEnv env0, Code code0) {
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
		FunctionContext fcx = new FunctionContext(env0.get(FunctionContext.class));
		env.add(FunctionContext.class, fcx);
		this.setStartIndex(fcx.enter());
		for (int i = 0; i < pnames.length; i++) {
			env.add(pnames[i], fcx.newVariable(pnames[i], pats[i]));
		}
		// Code code0 = env.parseCode(env, body);
		Code code = env.catchCode(() -> code0.asType(env, ret));
		this.setFieldMap(fcx.exit());
		if (dyn) {
			ret.toImmutable();
		}
		dom.reset();
		this.setParamTypes(
				Arrays.stream(dom.dupParamTypes(this.getParamTypes())).map(t -> t.finalTy()).toArray(Ty[]::new));
		this.setReturnType(dom.dupRetType(this.getReturnType()).finalTy());
		return code;
	}

}
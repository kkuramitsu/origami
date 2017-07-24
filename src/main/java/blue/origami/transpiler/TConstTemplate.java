package blue.origami.transpiler;

import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TNameCode;
import blue.origami.transpiler.code.TParamCode;

public class TConstTemplate extends TCodeTemplate {

	public TConstTemplate(String name, TType returnType, String template) {
		super(name, returnType, TConsts.emptyTypes, template);
	}

	@Override
	public TParamCode newParamCode(TEnv env, String name, TCode[] params) {
		return new TParamCode(this, params);
	}

	@Override
	public boolean isNameRef(TEnv env) {
		return !this.getReturnType().isUntyped();
	}

	@Override
	public TCode nameCode(TEnv env, String name) {
		return new TNameCode(this.getName(), this.getReturnType());
	}

}
package blue.origami.transpiler;

import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ExprCode;

public class TConstTemplate extends TCodeTemplate {

	public TConstTemplate(String name, Ty returnType, String template) {
		super(name, returnType, TArrays.emptyTypes, template);
	}

	@Override
	public boolean isNameInfo(TEnv env) {
		return !this.getReturnType().isUntyped();
	}

	@Override
	public Code nameCode(TEnv env, String name) {
		return new ExprCode(this, TArrays.emptyCodes);
		// return new TNameCode(this.getName(), this.getReturnType());
	}

}
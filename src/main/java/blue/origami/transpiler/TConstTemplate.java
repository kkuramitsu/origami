package blue.origami.transpiler;

import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TExprCode;

public class TConstTemplate extends TCodeTemplate {

	public TConstTemplate(String name, Ty returnType, String template) {
		super(name, returnType, TArrays.emptyTypes, template);
	}

	@Override
	public boolean isNameRef(TEnv env) {
		return !this.getReturnType().isUntyped();
	}

	@Override
	public TCode nameCode(TEnv env, String name) {
		return new TExprCode(this, TArrays.emptyCodes);
		// return new TNameCode(this.getName(), this.getReturnType());
	}

}
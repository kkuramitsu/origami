package blue.origami.transpiler;

import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ExprCode;
import blue.origami.transpiler.type.Ty;

public class ConstMap extends CodeMap {

	public ConstMap(String name, Ty returnType, String template) {
		super(0, name, template, returnType, TArrays.emptyTypes);
	}

	@Override
	public boolean isNameInfo(TEnv env) {
		return !this.getReturnType().isVoid();
	}

	@Override
	public Code newNameCode(TEnv env, AST s) {
		return new ExprCode(this, TArrays.emptyCodes).setSource(s);
	}

}
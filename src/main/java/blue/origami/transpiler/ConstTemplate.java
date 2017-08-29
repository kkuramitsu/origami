package blue.origami.transpiler;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ExprCode;
import blue.origami.transpiler.type.Ty;

public class ConstTemplate extends CodeTemplate {

	public ConstTemplate(String name, Ty returnType, String template) {
		super(name, returnType, TArrays.emptyTypes, template);
	}

	@Override
	public boolean isNameInfo(TEnv env) {
		return !this.getReturnType().isVoid();
	}

	@Override
	public Code newCode(Tree<?> s) {
		return new ExprCode(this, TArrays.emptyCodes).setSource(s);
	}

}
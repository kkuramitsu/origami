package blue.origami.transpiler;

import blue.origami.common.OArrays;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ExprCode;
import blue.origami.transpiler.type.Ty;
import origami.nez2.Token;

public class ConstMap extends CodeMap {

	public ConstMap(String name, String template, Ty returnType) {
		super(0, name, template, returnType, OArrays.emptyTypes);
	}

	@Override
	public boolean isNameInfo(Env env) {
		return !this.getReturnType().isVoid();
	}

	@Override
	public Code newNameCode(Env env, Token s) {
		return new ExprCode(this, OArrays.emptyCodes).setSource(s);
	}

}
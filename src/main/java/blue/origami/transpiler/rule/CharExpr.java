package blue.origami.transpiler.rule;

import blue.origami.common.OStringUtils;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.CharCode;
import blue.origami.transpiler.code.StringCode;

public class CharExpr implements ParseRule {

	@Override
	public Code apply(Env env, AST t) {
		String s = t.getString();
		if (s.length() == 1) {
			return new CharCode(s.charAt(0));
		}else if (s.length() == 0) {
			return new CharCode('\0');
		}
		return new StringCode(OStringUtils.unquoteString(s));
	}

}

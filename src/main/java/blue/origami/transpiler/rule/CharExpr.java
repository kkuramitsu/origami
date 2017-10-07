package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.IntCode.TCharCode;
import blue.origami.transpiler.code.StringCode;
import blue.origami.util.OStringUtils;

public class CharExpr implements ParseRule {

	@Override
	public Code apply(TEnv env, AST t) {
		String s = t.getString();
		if (s.length() == 1) {
			return new TCharCode(s.charAt(0));
		}
		return new StringCode(OStringUtils.unquoteString(s));
	}

}
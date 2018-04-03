package blue.origami.transpiler.rule;

import blue.origami.common.OStringUtils;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.IntCode.TCharCode;
import blue.origami.transpiler.code.StringCode;
import origami.nez2.ParseTree;

public class CharExpr implements ParseRule {

	@Override
	public Code apply(Env env, ParseTree t) {
		String s = t.asString();
		if (s.length() == 1) {
			return new TCharCode(s.charAt(0));
		}
		return new StringCode(OStringUtils.unquoteString(s));
	}

}
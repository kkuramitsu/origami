package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TIntCode.TCharCode;
import blue.origami.transpiler.code.TStringCode;
import blue.origami.util.OStringUtils;

public class CharExpr implements ParseRule {

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		String s = t.getString();
		if (s.length() == 1) {
			return new TCharCode(s.charAt(0));
		}
		return new TStringCode(OStringUtils.unquoteString(s));
	}

}
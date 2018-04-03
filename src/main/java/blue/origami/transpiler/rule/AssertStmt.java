package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ExprCode;
import origami.nez2.ParseTree;

public class AssertStmt implements ParseRule, Symbols {

	@Override
	public Code apply(Env env, ParseTree t) {
		Code cond = env.parseCode(env, t.get(_cond));
		return new ExprCode("assert", cond);
	}

}

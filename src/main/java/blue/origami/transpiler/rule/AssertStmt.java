package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ExprCode;

public class AssertStmt implements ParseRule, Symbols {

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		Code cond = env.parseCode(env, t.get(_cond));
		return new ExprCode("assert", cond);
	}

}

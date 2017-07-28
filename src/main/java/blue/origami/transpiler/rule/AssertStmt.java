package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.rule.OSymbols;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TExprCode;

public class AssertStmt implements TTypeRule, OSymbols {

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		TCode cond = env.parseCode(env, t.get(_cond));
		return new TExprCode("assert", cond);
	}

}

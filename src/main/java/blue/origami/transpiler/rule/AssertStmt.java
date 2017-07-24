package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.rule.OSymbols;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TParamCode;

public class AssertStmt implements TTypeRule, OSymbols {

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		TCode cond = env.typeExpr(env, t.get(_cond)).asType(env, TType.tBool);
		Template tp = env.getTemplate("assert", "assert (%s)");
		return new TParamCode(tp, cond);
	}

}

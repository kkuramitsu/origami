package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.rule.OSymbols;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.TCode;

public class BinaryExpr implements TTypeRule, OSymbols {

	final String op;

	public BinaryExpr(String op) {
		this.op = op;
	}

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		TCode left = env.typeTree(env, t.get(_left));
		TCode right = env.typeTree(env, t.get(_right));
		// syncType(env, left, right);
		// return left.op(env, this.op, right);
		return env.findParamCode(env, this.op, left, right);
	}

}

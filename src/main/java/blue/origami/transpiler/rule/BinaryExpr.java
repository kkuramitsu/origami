package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.rule.OSymbols;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.TCode;

public class BinaryExpr implements ParseRule, OSymbols {

	final String op;

	public BinaryExpr(String op) {
		this.op = op;
	}

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		TCode left = env.parseCode(env, t.get(_left));
		TCode right = env.parseCode(env, t.get(_right));
		return left.applyMethodCode(env, this.op, right);
	}

}

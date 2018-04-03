package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import origami.nez2.ParseTree;

public class UnaryExpr implements ParseRule, Symbols {

	final String op;

	public UnaryExpr(String op) {
		this.op = op;
	}

	@Override
	public Code apply(Env env, ParseTree t) {
		Code expr = env.parseCode(env, t.get(_expr));
		return expr.applyMethodCode(env, this.op);
	}

}
package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;

public class UnaryExpr implements ParseRule, OSymbols {

	final String op;

	public UnaryExpr(String op) {
		this.op = op;
	}

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		Code expr = env.parseCode(env, t.get(_expr));
		return expr.applyMethodCode(env, this.op);
	}

}
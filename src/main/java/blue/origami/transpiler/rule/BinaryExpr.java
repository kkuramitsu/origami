package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;

public class BinaryExpr implements ParseRule, OSymbols {

	final String op;

	public BinaryExpr(String op) {
		this.op = op;
	}

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		Code left = env.parseCode(env, t.get(_left));
		Code right = env.parseCode(env, t.get(_right));
		return left.applyMethodCode(env, this.op, right);
	}

}

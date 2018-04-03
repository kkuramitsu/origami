package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.BinaryCode;
import blue.origami.transpiler.code.Code;
import origami.nez2.ParseTree;

public class BinaryExpr implements ParseRule, Symbols {

	final String op;

	public BinaryExpr(String op) {
		this.op = op;
	}

	@Override
	public Code apply(Env env, ParseTree t) {
		Code left = env.parseCode(env, t.get(_left));
		Code right = env.parseCode(env, t.get(_right));
		return new BinaryCode(this.op, left, right).setSource(env.s(t.get(_right)));
	}

}

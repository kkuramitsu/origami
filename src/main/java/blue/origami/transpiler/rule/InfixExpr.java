package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.BinaryCode;
import blue.origami.transpiler.code.Code;

public class InfixExpr implements ParseRule, Symbols {

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		Code left = env.parseCode(env, t.get(_left));
		String op = t.getStringAt(_name, null);
		Code right = env.parseCode(env, t.get(_right));
		return new BinaryCode(op, left, right);
	}

}
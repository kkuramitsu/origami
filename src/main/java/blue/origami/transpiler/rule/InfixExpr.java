package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.BinaryCode;
import blue.origami.transpiler.code.Code;

public class InfixExpr implements ParseRule, Symbols {

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		Tree<?> name = t.get(_name);
		Code left = env.parseCode(env, t.get(_left));
		Code right = env.parseCode(env, t.get(_right));
		return new BinaryCode(name.getString(), left, right).setSource(name);
	}

}
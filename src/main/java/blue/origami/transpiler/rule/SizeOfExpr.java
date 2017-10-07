package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;

public class SizeOfExpr implements ParseRule, Symbols {
	@Override
	public Code apply(TEnv env, AST t) {
		Code recv = env.parseCode(env, t.get(_expr));
		return recv.applyMethodCode(env, "||");
	}
}
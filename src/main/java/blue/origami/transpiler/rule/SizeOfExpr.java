package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;

public class SizeOfExpr implements ParseRule, Symbols {
	@Override
	public Code apply(Env env, AST t) {
		Code recv = env.parseCode(env, t.get(_expr));
		return recv.applyMethodCode(env, "||");
	}
}
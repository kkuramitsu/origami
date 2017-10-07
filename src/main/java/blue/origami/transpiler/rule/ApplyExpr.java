package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;

public class ApplyExpr implements ParseRule, Symbols {

	@Override
	public Code apply(TEnv env, AST t) {
		Code[] params = env.parseSubCode(env, t.get(_param));
		Code recv = env.parseCode(env, t.get(_recv));
		return recv.applyCode(env, params);
	}
}

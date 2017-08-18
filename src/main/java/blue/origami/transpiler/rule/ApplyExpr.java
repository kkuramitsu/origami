package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;

public class ApplyExpr implements ParseRule, Symbols {

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		Code[] params = env.parseParams(env, t, _param);
		Code recv = env.parseCode(env, t.get(_recv));
		return recv.applyCode(env, params);
	}
}

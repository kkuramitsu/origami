package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import origami.nez2.ParseTree;

public class ApplyExpr implements ParseRule, Symbols {

	@Override
	public Code apply(Env env, ParseTree t) {
		Code[] params = env.parseSubCode(env, t.get(_param));
		Code recv = env.parseCode(env, t.get(_recv));
		return recv.applyCode(env, params);
	}
}

package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import origami.nez2.ParseTree;

public class MethodExpr implements ParseRule, Symbols {
	@Override
	public Code apply(Env env, ParseTree t) {
		Code recv = env.parseCode(env, t.get(_recv));
		String name = t.get(_name).asString();
		Code[] params = env.parseSubCode(env, t.get(_param));
		return recv.applyMethodCode(env, name, params);
	}
}
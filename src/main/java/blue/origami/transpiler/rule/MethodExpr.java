package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;

public class MethodExpr implements ParseRule, Symbols {
	@Override
	public Code apply(TEnv env, AST t) {
		Code recv = env.parseCode(env, t.get(_recv));
		String name = t.getStringAt(_name, "");
		Code[] params = env.parseSubCode(env, t.get(_param));
		return recv.applyMethodCode(env, name, params);
	}
}
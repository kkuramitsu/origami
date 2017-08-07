package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;

public class MethodExpr implements ParseRule, OSymbols {
	@Override
	public Code apply(TEnv env, Tree<?> t) {
		Code recv = env.parseCode(env, t.get(_recv));
		String name = t.getStringAt(_name, "");
		Code[] params = env.parseParams(env, t, _param);
		return recv.applyMethodCode(env, name, params);
	}
}
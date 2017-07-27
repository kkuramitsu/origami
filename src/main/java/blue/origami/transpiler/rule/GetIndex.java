package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.rule.OSymbols;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.TCode;

public class GetIndex implements TTypeRule, OSymbols {
	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		TCode recv = env.parseCode(env, t.get(_recv));
		TCode[] params = env.parseParams(env, t, _param);
		return recv.applyMethodCode(env, "[]", params);
	}
}
package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.rule.OSymbols;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TTypeCode;

public class FuncType implements ParseRule, OSymbols {
	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		TType ret = env.parseType(env, t.get(_base), null);
		Tree<?> params = t.get(_param);
		TType[] a = new TType[params.size()];
		for (int i = 0; i < params.size(); i++) {
			a[i] = env.parseType(env, params.get(i), null);
		}
		return new TTypeCode(TType.tFunc(ret, a));
	}
}
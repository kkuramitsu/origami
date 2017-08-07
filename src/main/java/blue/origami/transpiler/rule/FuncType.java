package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TypeCode;

public class FuncType implements ParseRule, Symbols {
	@Override
	public Code apply(TEnv env, Tree<?> t) {
		Ty ret = env.parseType(env, t.get(_base), null);
		Tree<?> params = t.get(_param);
		Ty[] a = new Ty[params.size()];
		for (int i = 0; i < params.size(); i++) {
			a[i] = env.parseType(env, params.get(i), null);
		}
		return new TypeCode(Ty.tFunc(ret, a));
	}
}
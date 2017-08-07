package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TErrorCode;
import blue.origami.transpiler.code.TTypeCode;

public class GenericType implements ParseRule, OSymbols {
	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		Ty ty = env.parseType(env, t.get(_base), null);
		String name = ty.toString();
		Tree<?> params = t.get(_param);
		Ty[] p = new Ty[params.size()];
		for (int i = 0; i < p.length; i++) {
			p[i] = env.parseType(env, params.get(i), null);
		}
		switch (name) {
		case "Option":
		case "Maybe":
			return new TTypeCode(Ty.tOption(p[0]));
		case "Dict":
			return new TTypeCode(Ty.tImDict(p[0]));
		case "Dict'":
			return new TTypeCode(Ty.tMDict(p[0]));
		case "Array":
			return new TTypeCode(Ty.tImArray(p[0]));
		case "Array'":
			return new TTypeCode(Ty.tMArray(p[0]));
		}
		return new TErrorCode(t.get(_base), TFmt.undefined_type__YY0, name);
	}
}
package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.TypeCode;

public class GenericType implements ParseRule, Symbols {
	@Override
	public Code apply(TEnv env, Tree<?> t) {
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
			return new TypeCode(Ty.tOption(p[0]));
		case "Dict":
			return new TypeCode(Ty.tImDict(p[0]));
		case "Dict'":
			return new TypeCode(Ty.tMDict(p[0]));
		case "Array":
			return new TypeCode(Ty.tImArray(p[0]));
		case "Array'":
			return new TypeCode(Ty.tMArray(p[0]));
		}
		return new ErrorCode(t.get(_base), TFmt.undefined_type__YY0, name);
	}
}
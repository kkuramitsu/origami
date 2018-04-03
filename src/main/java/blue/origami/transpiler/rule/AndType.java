package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.Ty;
import origami.nez2.ParseTree;

public class AndType implements ParseRule, Symbols {
	@Override
	public Code apply(Env env, ParseTree t) {
		Ty base = env.parseType(env, t.get(_base), null);
		Ty ty = env.parseType(env, t.get(_param), null);
		return new TypeCode(Ty.tCond(base, true, ty));
	}
}
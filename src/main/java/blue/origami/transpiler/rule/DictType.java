package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.Ty;

public class DictType implements ParseRule, Symbols {
	@Override
	public Code apply(Env env, AST t) {
		Ty ty = env.parseType(env, t.get(_base), null);
		return new TypeCode(Ty.tMonad("Dict'", ty));
	}
}
package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.Ty;

public class MonadType implements ParseRule, Symbols {
	@Override
	public Code apply(Env env, AST t) {
		String name = Ty.parseMonadName(t.get(_base));
		Ty ty = env.parseType(env, t.get(_param), null);
		return new TypeCode(Ty.tMonad(name, ty));
	}
}
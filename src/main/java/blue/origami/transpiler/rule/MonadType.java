package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.Ty;

public class MonadType implements ParseRule, Symbols {
	@Override
	public Code apply(TEnv env, Tree<?> t) {
		String name = Ty.parseMonadName(t.get(_param));
		Ty ty = env.parseType(env, t.get(_param), null);
		return new TypeCode(Ty.tMonad(name, ty));
	}
}
package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.Ty;

public class ArrayType implements ParseRule, Symbols {
	@Override
	public Code apply(TEnv env, AST t) {
		Ty ty = env.parseType(env, t.get(_base), null);
		return new TypeCode(Ty.tArray(ty));
	}
}

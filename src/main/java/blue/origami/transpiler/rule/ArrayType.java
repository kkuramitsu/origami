package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TypeCode;

public class ArrayType implements ParseRule, Symbols {
	boolean isMutable = true;

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		Ty ty = env.parseType(env, t.get(_base), null);
		return new TypeCode(this.isMutable ? Ty.tArray(ty) : Ty.tImArray(ty));
	}
}

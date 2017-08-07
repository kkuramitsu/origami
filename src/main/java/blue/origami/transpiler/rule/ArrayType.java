package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TTypeCode;

public class ArrayType implements ParseRule, OSymbols {
	boolean isMutable = true;

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		Ty ty = env.parseType(env, t.get(_base), null);
		return new TTypeCode(this.isMutable ? Ty.tMArray(ty) : Ty.tImArray(ty));
	}
}

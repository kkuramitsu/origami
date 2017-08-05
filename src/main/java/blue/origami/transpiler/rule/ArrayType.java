package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.rule.OSymbols;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TTypeCode;

public class ArrayType implements ParseRule, OSymbols {
	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		TType ty = env.parseType(env, t.get(_base), null);
		return new TTypeCode(TType.tArray(ty));
	}
}
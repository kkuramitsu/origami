package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.rule.OSymbols;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TErrorCode;
import blue.origami.transpiler.code.TTypeCode;

public class ExprType implements TTypeRule, OSymbols {
	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		TCode c = env.parseCode(env, t.get(_expr)).asType(env, TType.tUntyped);
		if (c.getType().isUntyped()) {
			throw new TErrorCode(t.get(_expr), TFmt.failed_type_inference);
		}
		return new TTypeCode(c.getType());
	}
}
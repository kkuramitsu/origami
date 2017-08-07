package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.TypeCode;

public class ExprType implements ParseRule, OSymbols {
	@Override
	public Code apply(TEnv env, Tree<?> t) {
		Code c = env.parseCode(env, t.get(_expr)).asType(env, Ty.tUntyped);
		if (c.getType().isUntyped()) {
			throw new ErrorCode(t.get(_expr), TFmt.failed_type_inference);
		}
		return new TypeCode(c.getType());
	}
}
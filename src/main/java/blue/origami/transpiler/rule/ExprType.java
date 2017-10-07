package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.Ty;

public class ExprType implements ParseRule, Symbols {
	@Override
	public Code apply(TEnv env, AST t) {
		Ty req = Ty.tUntyped();
		Code c = env.parseCode(env, t.get(_expr)).asType(env, req);
		if (c.getType() == req) {
			throw new ErrorCode(t.get(_expr), TFmt.failed_type_inference);
		}
		return new TypeCode(c.getType());
	}
}
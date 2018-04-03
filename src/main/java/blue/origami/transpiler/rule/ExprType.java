package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.Ty;
import origami.nez2.ParseTree;

public class ExprType implements ParseRule, Symbols {
	@Override
	public Code apply(Env env, ParseTree t) {
		Ty req = Ty.tVar(null);
		Code c = env.parseCode(env, t.get(_expr)).asType(env, req);
		if (c.getType() == req) {
			throw new ErrorCode(env.s(t.get(_expr)), TFmt.failed_type_inference);
		}
		return new TypeCode(c.getType());
	}
}
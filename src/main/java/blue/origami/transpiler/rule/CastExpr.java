package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.CastCode;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.type.Ty;
import origami.nez2.ParseTree;

public class CastExpr implements ParseRule, Symbols {

	@Override
	public Code apply(Env env, ParseTree t) {
		Code expr = env.parseCode(env, t.get(_recv));
		ParseTree type = t.get(_type);
		String name = type.asString();
		Ty ty = env.parseType(env, type, () -> {
			throw new ErrorCode(env.s(type), TFmt.undefined_type__YY1, name);
		});
		return new CastCode(ty, expr).setSource(env.s(type));
	}
}
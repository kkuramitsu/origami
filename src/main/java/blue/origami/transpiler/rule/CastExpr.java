package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.CastCode;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.type.Ty;

public class CastExpr implements ParseRule, Symbols {

	@Override
	public Code apply(Env env, AST t) {
		Code expr = env.parseCode(env, t.get(_recv));
		AST type = t.get(_type);
		String name = type.getString();
		// switch (name) {
		// case "Mutable":
		// new MutableCode(expr).setSource(type);
		// }
		Ty ty = env.parseType(env, type, () -> {
			throw new ErrorCode(type, TFmt.undefined_type__YY1, name);
		});
		return new CastCode(ty, expr).setSource(type);
	}
}
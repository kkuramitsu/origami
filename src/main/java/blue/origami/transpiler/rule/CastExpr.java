package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.CastCode;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.type.Ty;

public class CastExpr implements ParseRule, Symbols {

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		Code expr = env.parseCode(env, t.get(_expr));
		Tree<?> type = t.get(_type);
		String name = type.getString();
		// switch (name) {
		// case "Mutable":
		// new MutableCode(expr).setSource(type);
		// }
		Ty ty = env.parseType(env, type, () -> {
			throw new ErrorCode(type, TFmt.undefined_type__YY0, name);
		});
		return new CastCode(ty, expr).setSource(type);
	}
}
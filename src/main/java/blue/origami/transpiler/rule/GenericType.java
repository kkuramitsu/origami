package blue.origami.transpiler.rule;

import java.util.Arrays;

import blue.origami.common.ODebug;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.Ty;
import origami.nez2.ParseTree;

public class GenericType implements ParseRule, Symbols {
	@Override
	public Code apply(Env env, ParseTree t) {
		Ty ty = env.parseType(env, t.get(_base), null);
		String name = ty.toString();
		Ty[] p = Arrays.stream(t.get(_param).asArray()).map(x -> env.parseType(env, x, null)).toArray(Ty[]::new);
		switch (name) {
		case "Option":
		case "Maybe":
			return new TypeCode(Ty.tOption(p[0]));
		case "List":
			return new TypeCode(Ty.tList(p[0]));
		default:
			ODebug.TODO();
			// if (Ty.isDefinedMonad(name)) {
			// return new TypeCode(Ty.tMonad(name, p[0]));
			// }
		}
		return new ErrorCode(env.s(t.get(_base)), TFmt.undefined_type__YY1, name);
	}
}
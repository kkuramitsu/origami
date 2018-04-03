package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.Language;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.Ty;
import origami.nez2.ParseTree;

public class ClassType implements ParseRule {
	@Override
	public Code apply(Env env, ParseTree t) {
		Language lang = env.getLanguage();
		Ty type = lang.findType(env, t.asString());
		if (type == null) {
			throw new ErrorCode(env.s(t), TFmt.undefined_type__YY1, t.asString());
		}
		return new TypeCode(type);
	}

}

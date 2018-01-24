package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.Language;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.Ty;

public class ClassType implements ParseRule {
	@Override
	public Code apply(Env env, AST t) {
		Language lang = env.getLanguage();
		Ty type = lang.findType(env, t.getString());
		if (type == null) {
			throw new ErrorCode(t, TFmt.undefined_type__YY1, t.getString());
		}
		return new TypeCode(type);
	}

}

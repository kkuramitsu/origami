package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.Ty;

public class TagType implements ParseRule, Symbols {

	@Override
	public Code apply(Env env, AST t) {
		Ty inner = env.parseType(env, t.get(_type), null);
		String tag = t.get(_name).getString();
		return new TypeCode(Ty.tTag(inner, tag));
	}
}
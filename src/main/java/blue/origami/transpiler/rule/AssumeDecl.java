package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DoneCode;
import blue.origami.transpiler.type.Ty;

public class AssumeDecl implements ParseRule, Symbols {

	@Override
	public Code apply(Env env, AST t) {
		for (AST sub : t.get(_body)) {
			Ty type = env.parseType(env, sub.get(_type), null);
			String[] names = this.parseNames(sub.get(_name));
			env.addNameDecl(env.getTranspiler(), names, type);
		}
		return new DoneCode();
	}

	final static String[] emptyNames = new String[0];

	String[] parseNames(AST names) {
		if (names == null) {
			return emptyNames;
		}
		String[] p = new String[names.size()];
		int i = 0;
		for (AST sub : names) {
			p[i] = sub.getString();
			i++;
		}
		return p;
	}

}

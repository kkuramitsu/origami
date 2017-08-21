package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DeclCode;
import blue.origami.transpiler.type.Ty;

public class AssumeDecl implements ParseRule, Symbols {

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		for (Tree<?> sub : t.get(_body)) {
			Ty type = env.parseType(env, sub.get(_type), null);
			String[] names = this.parseNames(sub.get(_name));
			env.addNameDecl(env.getTranspiler(), names, type);
		}
		return new DeclCode();
	}

	final static String[] emptyNames = new String[0];

	String[] parseNames(Tree<?> names) {
		if (names == null) {
			return emptyNames;
		}
		String[] p = new String[names.size()];
		int i = 0;
		for (Tree<?> sub : names) {
			p[i] = sub.getString();
			i++;
		}
		return p;
	}

}

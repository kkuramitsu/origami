package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.NameCode;

public class NameExpr implements ParseRule {

	@Override
	public Code apply(Env env, AST t) {
		// String name = t.getString();
		// env.addParsedName(name);
		return new NameCode(t);
	}

	public interface NameInfo {
		public boolean isNameInfo(Env env);

		public void used(Env env);

		public Code newNameCode(Env env, AST s);
	}

}

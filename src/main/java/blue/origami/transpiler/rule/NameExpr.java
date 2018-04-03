package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.VarNameCode;
import origami.nez2.ParseTree;
import origami.nez2.Token;

public class NameExpr implements ParseRule {

	@Override
	public Code apply(Env env, ParseTree t) {
		// String name = t.getString();
		// env.addParsedName(name);
		return new VarNameCode(env.s(t));
	}

	public interface NameInfo {
		public boolean isNameInfo(Env env);

		public void used(Env env);

		public Code newNameCode(Env env, Token s);
	}

}

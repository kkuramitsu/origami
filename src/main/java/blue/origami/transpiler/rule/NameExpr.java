package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.NameCode;

public class NameExpr implements ParseRule {

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		// String name = t.getString();
		// env.addParsedName(name);
		return new NameCode(t);
	}

	public interface NameInfo {
		public boolean isNameInfo(TEnv env);

		public void used(TEnv env);

		public Code newCode(TEnv env, Tree<?> s);
	}

}

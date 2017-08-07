package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.NameCode;

public class NameExpr implements ParseRule {

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		return new NameCode(t);
	}

	public interface TNameRef {
		public boolean isNameRef(TEnv env);

		public Code nameCode(TEnv env, String name);
	}

}

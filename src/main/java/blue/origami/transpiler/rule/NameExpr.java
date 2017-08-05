package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TNameCode;

public class NameExpr implements ParseRule {

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		return new TNameCode(t);
	}

	public interface TNameRef {
		public boolean isNameRef(TEnv env);

		public TCode nameCode(TEnv env, String name);
	}

}

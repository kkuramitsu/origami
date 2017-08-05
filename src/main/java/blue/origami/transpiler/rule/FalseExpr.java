package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.TBoolCode;
import blue.origami.transpiler.code.TCode;

public class FalseExpr implements ParseRule {

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		return new TBoolCode(false);
	}

}

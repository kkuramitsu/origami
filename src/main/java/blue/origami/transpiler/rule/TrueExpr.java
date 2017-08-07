package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.BoolCode;
import blue.origami.transpiler.code.Code;

public class TrueExpr implements ParseRule {

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		return new BoolCode(true);
	}

}
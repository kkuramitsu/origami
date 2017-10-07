package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.BoolCode;
import blue.origami.transpiler.code.Code;

public class TrueExpr implements ParseRule {

	@Override
	public Code apply(TEnv env, AST t) {
		return new BoolCode(true);
	}

}
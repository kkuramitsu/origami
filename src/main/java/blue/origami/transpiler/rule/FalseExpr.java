package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.BoolCode;
import blue.origami.transpiler.code.Code;

public class FalseExpr implements ParseRule {

	@Override
	public Code apply(Env env, AST t) {
		return new BoolCode(false);
	}

}

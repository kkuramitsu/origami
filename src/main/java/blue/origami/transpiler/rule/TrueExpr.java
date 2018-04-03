package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.BoolCode;
import blue.origami.transpiler.code.Code;
import origami.nez2.ParseTree;

public class TrueExpr implements ParseRule {

	@Override
	public Code apply(Env env, ParseTree t) {
		return new BoolCode(true);
	}

}
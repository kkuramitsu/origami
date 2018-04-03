package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.MultiCode;
import origami.nez2.ParseTree;

public class EmptyExpr implements ParseRule {

	@Override
	public Code apply(Env env, ParseTree t) {
		return new MultiCode();
	}
}
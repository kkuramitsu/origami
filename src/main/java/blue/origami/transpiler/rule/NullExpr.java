package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.NoneCode;
import origami.nez2.ParseTree;

public class NullExpr implements ParseRule {

	@Override
	public Code apply(Env env, ParseTree t) {
		return new NoneCode(env.s(t));
	}
}

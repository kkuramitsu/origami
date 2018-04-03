package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ListCode;
import origami.nez2.ParseTree;

public class ListExpr extends LoggerRule implements Symbols, ParseRule {

	@Override
	public Code apply(Env env, ParseTree t) {
		return new ListCode(env.parseSubCode(env, t));
	}
}
package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TupleCode;
import origami.nez2.ParseTree;

public class TupleExpr extends LoggerRule implements Symbols, ParseRule {

	@Override
	public Code apply(Env env, ParseTree t) {
		return new TupleCode(env.parseSubCode(env, t));
	}

}
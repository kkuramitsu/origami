package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import origami.nez2.ParseTree;

public class SizeOfExpr implements ParseRule, Symbols {
	@Override
	public Code apply(Env env, ParseTree t) {
		Code recv = env.parseCode(env, t.get(_expr));
		return recv.applyMethodCode(env, "||");
	}
}
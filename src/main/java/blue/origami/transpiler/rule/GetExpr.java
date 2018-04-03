package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.GetCode;
import origami.nez2.ParseTree;

public class GetExpr implements ParseRule, Symbols {
	@Override
	public Code apply(Env env, ParseTree t) {
		Code recv = env.parseCode(env, t.get(_recv));
		return new GetCode(recv, env.s(t.get(_name)));
	}

}

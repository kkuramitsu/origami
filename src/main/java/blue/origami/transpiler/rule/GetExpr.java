package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.GetCode;

public class GetExpr implements ParseRule, Symbols {
	@Override
	public Code apply(Env env, AST t) {
		Code recv = env.parseCode(env, t.get(_recv));
		return new GetCode(recv, t.get(_name));
	}

}

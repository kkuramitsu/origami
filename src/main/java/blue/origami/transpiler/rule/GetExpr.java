package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.GetCode;

public class GetExpr implements ParseRule, Symbols {
	@Override
	public Code apply(TEnv env, AST t) {
		Code recv = env.parseCode(env, t.get(_recv));
		return new GetCode(recv, t.get(_name));
	}

}

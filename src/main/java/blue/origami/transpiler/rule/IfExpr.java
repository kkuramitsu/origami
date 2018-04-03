package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.IfCode;
import blue.origami.transpiler.code.MultiCode;
import origami.nez2.ParseTree;

public class IfExpr implements ParseRule, Symbols {

	@Override
	public Code apply(Env env, ParseTree t) {
		Code condCode = env.parseCode(env, t.get(_cond));
		Code thenCode = env.parseCode(env, t.get(_then));
		Code elseCode = t.has(_else) ? env.parseCode(env, t.get(_else)) : new MultiCode();
		return new IfCode(condCode, thenCode, elseCode);
	}

}

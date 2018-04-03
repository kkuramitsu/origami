package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.BinaryCode;
import blue.origami.transpiler.code.Code;
import origami.nez2.ParseTree;

public class InfixExpr implements ParseRule, Symbols {

	@Override
	public Code apply(Env env, ParseTree t) {
		ParseTree name = t.get(_name);
		Code left = env.parseCode(env, t.get(_left));
		Code right = env.parseCode(env, t.get(_right));
		return new BinaryCode(name.asString(), left, right).setSource(env.s(name));
	}

}
package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.SourceCode;
import origami.nez2.ParseTree;

public class SourceUnit implements ParseRule {

	@Override
	public Code apply(Env env, ParseTree t) {
		return new SourceCode(env.getTranspiler(), env.parseSubCode(env, t));
	}

}

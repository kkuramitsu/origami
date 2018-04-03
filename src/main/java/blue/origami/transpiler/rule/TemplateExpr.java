package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TemplateCode;
import origami.nez2.ParseTree;

public class TemplateExpr implements ParseRule {

	@Override
	public Code apply(Env env, ParseTree t) {
		return new TemplateCode(env.parseSubCode(env, t));
	}

}

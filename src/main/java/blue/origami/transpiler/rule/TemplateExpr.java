package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TemplateCode;

public class TemplateExpr implements ParseRule {

	@Override
	public Code apply(TEnv env, AST t) {
		return new TemplateCode(t.subMap(sub -> env.parseCode(env, sub), Code[]::new));
	}

}

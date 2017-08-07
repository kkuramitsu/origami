package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TemplateCode;

public class TemplateExpr implements ParseRule {

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		Code[] elements = new Code[t.size()];
		int c = 0;
		for (Tree<?> sub : t) {
			elements[c] = env.parseCode(env, sub).asType(env, Ty.tString);
			c++;
		}
		return new TemplateCode(elements);
	}

}

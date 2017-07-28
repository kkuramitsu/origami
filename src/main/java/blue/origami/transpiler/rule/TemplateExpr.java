package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TemplateCode;

public class TemplateExpr implements TTypeRule {

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		TCode[] elements = new TCode[t.size()];
		int c = 0;
		for (Tree<?> sub : t) {
			elements[c] = env.parseCode(env, sub).asType(env, TType.tString);
			c++;
		}
		return new TemplateCode(elements);
	}

}

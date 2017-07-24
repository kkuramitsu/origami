package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.TCode;
import blue.origami.util.ODebug;

public class TemplateExpr implements TTypeRule {

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		ODebug.TODO(this);
		return null;
	}

}

package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.MultiCode;

public class EmptyExpr implements ParseRule {

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		return new MultiCode();
	}
}
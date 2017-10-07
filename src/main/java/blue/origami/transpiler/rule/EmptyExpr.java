package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.MultiCode;

public class EmptyExpr implements ParseRule {

	@Override
	public Code apply(TEnv env, AST t) {
		return new MultiCode();
	}
}
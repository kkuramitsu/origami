package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.NoneCode;

public class NullExpr implements ParseRule {

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		return new NoneCode(t);
	}
}

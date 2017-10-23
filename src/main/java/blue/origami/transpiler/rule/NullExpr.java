package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.NoneCode;

public class NullExpr implements ParseRule {

	@Override
	public Code apply(Env env, AST t) {
		return new NoneCode(t);
	}
}

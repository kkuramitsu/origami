package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ListCode;

public class ListExpr extends LoggerRule implements Symbols, ParseRule {

	boolean isMutable;

	public ListExpr() {
		this(false);
	}

	public ListExpr(boolean isMutable) {
		this.isMutable = isMutable;
	}

	@Override
	public Code apply(Env env, AST t) {
		return new ListCode(this.isMutable, env.parseSubCode(env, t));
	}
}
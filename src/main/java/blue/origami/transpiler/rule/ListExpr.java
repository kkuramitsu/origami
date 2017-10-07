package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataListCode;

public class ListExpr extends LoggerRule implements Symbols, ParseRule {

	boolean isMutable;

	public ListExpr() {
		this(false);
	}

	public ListExpr(boolean isMutable) {
		this.isMutable = isMutable;
	}

	@Override
	public Code apply(TEnv env, AST t) {
		return new DataListCode(this.isMutable, env.parseSubCode(env, t));
	}
}
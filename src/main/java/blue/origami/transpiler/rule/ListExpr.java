package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataArrayCode;

public class ListExpr extends LoggerRule implements Symbols, ParseRule {

	boolean isMutable;

	public ListExpr() {
		this(false);
	}

	public ListExpr(boolean isMutable) {
		this.isMutable = isMutable;
	}

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		int c = 0;
		Code[] values = new Code[t.size()];
		for (Tree<?> sub : t) {
			values[c] = env.parseCode(env, sub);
			c++;
		}
		return new DataArrayCode(this.isMutable, values);
	}
}
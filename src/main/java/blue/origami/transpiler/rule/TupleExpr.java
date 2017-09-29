package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TupleCode;

public class TupleExpr extends LoggerRule implements Symbols, ParseRule {

	public TupleExpr() {
	}

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		Code[] values = new Code[t.size()];
		int c = 0;
		for (Tree<?> p : t) {
			values[c] = env.parseCode(env, p);
			c++;
		}
		return new TupleCode(values);
	}

}
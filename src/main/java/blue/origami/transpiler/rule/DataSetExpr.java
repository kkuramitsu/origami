package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.rule.OSymbols;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.TDataArrayCode;
import blue.origami.transpiler.code.TCode;

public class DataSetExpr extends LoggerRule implements OSymbols, ParseRule {

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		int c = 0;
		TCode[] values = new TCode[t.size()];
		for (Tree<?> sub : t) {
			values[c] = env.parseCode(env, sub);
			c++;
		}
		return new TDataArrayCode(values);
	}
}
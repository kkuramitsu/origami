package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataArrayCode;

public class DataSetExpr extends LoggerRule implements OSymbols, ParseRule {

	boolean isMutable = true;

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
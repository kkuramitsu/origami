package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.rule.OSymbols;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TDataCode;

public class DataExpr extends LoggerRule implements OSymbols, ParseRule {

	boolean isMutable = true;

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		String[] names = new String[t.size()];
		TCode[] values = new TCode[names.length];
		int c = 0;
		for (Tree<?> keyvalue : t) {
			names[c] = keyvalue.getStringAt(_name, "");
			values[c] = env.parseCode(env, keyvalue.get(_value));
			c++;
		}
		return new TDataCode(this.isMutable, names, values);
	}

}

package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataCode;

public class DataExpr extends LoggerRule implements Symbols, ParseRule {

	boolean isMutable = true;

	public DataExpr() {
		this(true);
	}

	public DataExpr(boolean isMutable) {
		this.isMutable = isMutable;
	}

	@Override
	public Code apply(TEnv env, AST t) {
		String[] names = new String[t.size()];
		Code[] values = new Code[names.length];
		int c = 0;
		for (AST keyvalue : t) {
			names[c] = keyvalue.getStringAt(_name, "");
			values[c] = env.parseCode(env, keyvalue.get(_value));
			c++;
		}
		return new DataCode(this.isMutable, names, values);
	}

}

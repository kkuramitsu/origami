package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DictCode;

public class DictExpr extends LoggerRule implements Symbols, ParseRule {

	boolean isMutable;

	public DictExpr() {
		this(false);
	}

	public DictExpr(boolean isMutable) {
		this.isMutable = isMutable;
	}

	@Override
	public Code apply(Env env, AST t) {
		String[] names = new String[t.size()];
		Code[] values = new Code[names.length];
		int c = 0;
		for (AST keyvalue : t) {
			names[c] = keyvalue.getStringAt(_name, "");
			values[c] = env.parseCode(env, keyvalue.get(_value));
			c++;
		}
		return new DictCode(this.isMutable, names, values);
	}
}
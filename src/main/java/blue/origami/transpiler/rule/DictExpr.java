package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataDictCode;

public class DictExpr extends LoggerRule implements Symbols, ParseRule {

	boolean isMutable;

	public DictExpr() {
		this(false);
	}

	public DictExpr(boolean isMutable) {
		this.isMutable = isMutable;
	}

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		String[] names = new String[t.size()];
		Code[] values = new Code[names.length];
		int c = 0;
		for (Tree<?> keyvalue : t) {
			names[c] = keyvalue.getStringAt(_name, "");
			values[c] = env.parseCode(env, keyvalue.get(_value));
			c++;
		}
		return new DataDictCode(this.isMutable, names, values);
	}
}
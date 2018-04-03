package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DictCode;
import origami.nez2.ParseTree;

public class DictExpr extends LoggerRule implements Symbols, ParseRule {

	@Override
	public Code apply(Env env, ParseTree t) {
		String[] names = new String[t.size()];
		Code[] values = new Code[names.length];
		int c = 0;
		for (ParseTree keyvalue : t.asArray()) {
			names[c] = keyvalue.get(_name).asString();
			values[c] = env.parseCode(env, keyvalue.get(_value));
			c++;
		}
		return new DictCode(names, values);
	}
}
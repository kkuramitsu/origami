package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.BoolCode;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataCode;
import origami.nez2.ParseTree;
import origami.nez2.Token;

public class DataExpr extends LoggerRule implements Symbols, ParseRule {

	@Override
	public Code apply(Env env, ParseTree t) {
		Token[] names = new Token[t.size()];
		Code[] values = new Code[names.length];
		int c = 0;
		for (ParseTree keyvalue : t.asArray()) {
			names[c] = env.s(keyvalue.get(_name));
			if (keyvalue.has(_value)) {
				values[c] = env.parseCode(env, keyvalue.get(_value));
			} else {
				values[c] = BoolCode.True;
			}
			c++;
		}
		return new DataCode(names, values);
	}

}

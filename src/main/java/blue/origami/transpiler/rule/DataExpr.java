package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.BoolCode;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataCode;

public class DataExpr extends LoggerRule implements Symbols, ParseRule {

	@Override
	public Code apply(Env env, AST t) {
		AST[] names = new AST[t.size()];
		Code[] values = new Code[names.length];
		int c = 0;
		for (AST keyvalue : t) {
			names[c] = keyvalue.get(_name);
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

package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.rule.OSymbols;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TDataCode;
import blue.origami.util.ODebug;

public class DataExpr extends LoggerRule implements OSymbols, TTypeRule {

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		String[] names = new String[t.size()];
		TCode[] values = new TCode[names.length];
		int c = 0;
		for (Tree<?> keyvalue : t) {
			String key = keyvalue.getStringAt(_name, "");
			TCode value = env.parseCode(env, keyvalue.get(_value));
			TType ty = env.findTypeHint(env, key);
			if (ty != null) {
				value = value.asType(env, ty);
			} else {
				value = value.asType(env, TType.tUntyped);
				ty = value.guessType();
				ODebug.trace("undefined symbol %s as %s", key, ty);
				env.addTypeHint(env, key, ty);
			}
			names[c] = key;
			values[c] = value;
			c++;
		}
		return new TDataCode(names, values);
	}
}

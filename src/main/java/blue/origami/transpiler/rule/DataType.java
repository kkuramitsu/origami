package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.Ty;
import origami.nez2.ParseTree;

public class DataType implements ParseRule {
	@Override
	public Code apply(Env env, ParseTree t) {
		String[] names = new String[t.size()];
		int c = 0;
		for (ParseTree sub : t.asArray()) {
			String name = sub.asString();
			// No check
			// Ty ty = env.findNameHint(name);
			// if (ty == null) {
			// throw new ErrorCode(sub, TFmt.no_type_hint__YY1, name);
			// }
			names[c] = name;
			c++;
		}
		return new TypeCode(Ty.tData(names));
	}
}
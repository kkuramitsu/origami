package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.Ty;

public class DataType implements ParseRule {
	@Override
	public Code apply(Env env, AST t) {
		String[] names = new String[t.size()];
		int c = 0;
		for (AST sub : t) {
			String name = sub.getString();
			Ty ty = env.findNameHint(name);
			if (ty == null) {
				throw new ErrorCode(sub, TFmt.no_type_hint__YY1, name);
			}
			names[c] = name;
			c++;
		}
		return new TypeCode(Ty.tData(names));
	}
}
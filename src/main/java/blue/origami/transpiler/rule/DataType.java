package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.Ty;

public class DataType implements ParseRule {
	boolean isMutable = true;

	public DataType() {
		this(true);
	}

	public DataType(boolean isMutable) {
		this.isMutable = isMutable;
	}

	@Override
	public Code apply(Env env, AST t) {
		String[] names = new String[t.size()];
		int c = 0;
		for (AST sub : t) {
			String name = sub.getString();
			NameHint hint = env.findGlobalNameHint(env, name);
			if (hint == null) {
				throw new ErrorCode(sub, TFmt.undefined_name__YY1, name);
			}
			names[c] = name;
			c++;
		}
		//return new TypeCode(this.isMutable ? Ty.tData(names) : Ty.tRecord(names));
		return new TypeCode(Ty.tData(this.isMutable, names));
	}
}

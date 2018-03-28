package blue.origami.transpiler.code;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.type.Ty;
import origami.libnez.OStrings;

public interface ValueCode extends Code {
	public Object getValue();

	@Override
	public default Code bindAs(Env env, AST ns, Ty ret) {
		return this.asType(env, ret);
	}

	@Override
	public default void strOut(StringBuilder sb) {
		OStrings.appendQuoted(sb, this.getValue());
	}

	public static Code[] values(String... values) {
		Code[] v = new Code[values.length];
		int c = 0;
		for (String s : values) {
			v[c] = new StringCode(s);
			c++;
		}
		return v;
	}

}

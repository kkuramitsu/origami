package blue.origami.transpiler.code;

import blue.origami.util.StringCombinator;

public interface ValueCode extends Code {
	public Object getValue();

	@Override
	public default void strOut(StringBuilder sb) {
		StringCombinator.appendQuoted(sb, this.getValue());
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

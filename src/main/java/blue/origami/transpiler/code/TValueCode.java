package blue.origami.transpiler.code;

import blue.origami.transpiler.TEnv;

public interface TValueCode extends TCode {
	public Object getValue();

	@Override
	public default String strOut(TEnv env) {
		return this.getTemplate(env).format(this.getValue());
	}

	public static TCode[] values(String... values) {
		TCode[] v = new TCode[values.length];
		int c = 0;
		for (String s : values) {
			v[c] = new TStringCode(s);
			c++;
		}
		return v;
	}

}

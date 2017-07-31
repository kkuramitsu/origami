package blue.origami.transpiler.code;

public interface TValueCode extends TCode {
	public Object getValue();

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

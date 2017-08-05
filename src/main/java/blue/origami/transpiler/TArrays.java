package blue.origami.transpiler;

import blue.origami.transpiler.code.TCode;

public class TArrays {
	// avoid duplicated empty array;
	public static final TInst[] emptyInsts = new TInst[0];
	public static final TType[] emptyTypes = new TType[0];
	public static final TCode[] emptyCodes = new TCode[0];
	public static final String[] emptyNames = new String[0];

	public static TType[] join(TType first, TType... params) {
		TType[] p = new TType[params.length + 1];
		p[0] = first;
		System.arraycopy(params, 0, p, 1, params.length);
		return p;
	}

	public static TCode[] join(TCode first, TCode... params) {
		TCode[] p = new TCode[params.length + 1];
		p[0] = first;
		System.arraycopy(params, 0, p, 1, params.length);
		return p;
	}

	public static TType[] ltrim(TType... params) {
		TType[] p = new TType[params.length - 1];
		System.arraycopy(params, 1, p, 0, params.length - 1);
		return p;
	}
}

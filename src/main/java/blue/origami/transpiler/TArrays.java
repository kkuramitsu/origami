package blue.origami.transpiler;

import blue.origami.transpiler.code.TCode;

public class TArrays {
	// avoid duplicated empty array;
	public static final TInst[] emptyInsts = new TInst[0];
	public static final Ty[] emptyTypes = new Ty[0];
	public static final TCode[] emptyCodes = new TCode[0];
	public static final String[] emptyNames = new String[0];

	public static Ty[] join(Ty first, Ty... params) {
		Ty[] p = new Ty[params.length + 1];
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

	public static Ty[] ltrim(Ty... params) {
		Ty[] p = new Ty[params.length - 1];
		System.arraycopy(params, 1, p, 0, params.length - 1);
		return p;
	}

	public static TCode[] ltrim(TCode... params) {
		TCode[] p = new TCode[params.length - 1];
		System.arraycopy(params, 1, p, 0, params.length - 1);
		return p;
	}
}

package blue.origami.transpiler;

import blue.origami.transpiler.code.TCode;

public class EmptyConstants {
	// avoid duplicated empty array;
	public static final TInst[] emptyInsts = new TInst[0];
	public static final TType[] emptyTypes = new TType[0];
	public static final TCode[] emptyCodes = new TCode[0];
}

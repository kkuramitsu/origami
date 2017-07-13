package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeTemplate;
import blue.origami.transpiler.TType;

public class TCastCode extends TUnaryCode {
	private final int mapCost;

	public TCastCode(TType ret, TMapTemplate tt, TCode inner) {
		super(ret, tt, inner);
		this.mapCost = tt.mapCost;
	}

	public int getMapCost() {
		return this.mapCost;
	}

	// constants
	public static final int SAME = 0;
	public static final int BESTCAST = 1;
	public static final int CAST = 3;
	public static final int BESTCONV = 8;
	public static final int CONV = 12;
	public static final int DOWNCAST = 128;
	public static final int STUPID = 256;

	public static class TMapTemplate extends TCodeTemplate {

		public static final TMapTemplate Stupid = new TMapTemplate("", TType.tUntyped, TType.tUntyped, STUPID, "%s");
		// fields
		protected int mapCost;

		public TMapTemplate(String name, TType fromType, TType returnType, int mapCost, String template) {
			super(name, returnType, new TType[] { fromType }, template);
			this.mapCost = mapCost;
		}

	}

}

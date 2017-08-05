package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TCodeTemplate;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;

public class TCastCode extends Code1 {
	public TCastCode(TType ret, TConvTemplate tp, TCode inner) {
		super(ret, inner);
		this.setTemplate(tp);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushCast(env, this);
	}

	// constants
	public static final int SAME = 0;
	public static final int BESTCAST = 1;
	public static final int CAST = 3;
	public static final int BESTCONV = 8;
	public static final int CONV = 12;
	public static final int BADCONV = 64;
	public static final int DOWNCAST = 64;
	public static final int STUPID = 256;

	public static class TConvTemplate extends TCodeTemplate {

		public static final TConvTemplate Stupid = new TConvTemplate("", TType.tUntyped, TType.tUntyped, STUPID, "%s");
		// fields
		protected int mapCost;

		public TConvTemplate(String name, TType fromType, TType returnType, int mapCost, String template) {
			super(name, returnType, new TType[] { fromType }, template);
			this.mapCost = mapCost;
		}

	}

	public static class TBoxCode extends TCastCode {

		public TBoxCode(TType ret, TCode inner) {
			super(ret, null, inner);
		}

	}

	public static class TUnboxCode extends TCastCode {

		public TUnboxCode(TType ret, TCode inner) {
			super(ret, null, inner);
		}

	}

}

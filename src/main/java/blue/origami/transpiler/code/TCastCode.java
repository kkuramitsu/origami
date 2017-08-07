package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TCodeTemplate;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Ty;
import blue.origami.util.StringCombinator;

public class TCastCode extends Code1 {
	public TCastCode(Ty ret, TConvTemplate tp, TCode inner) {
		super(ret, inner);
		this.setTemplate(tp);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushCast(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("(");
		StringCombinator.append(sb, this.getType());
		sb.append(")");
		StringCombinator.append(sb, this.getInner());
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

		public static final TConvTemplate Stupid = new TConvTemplate("", Ty.tUntyped, Ty.tUntyped, STUPID, "%s");
		// fields
		protected int mapCost;

		public TConvTemplate(String name, Ty fromType, Ty returnType, int mapCost, String template) {
			super(name, returnType, new Ty[] { fromType }, template);
			this.mapCost = mapCost;
		}

	}

	public static class TBoxCode extends TCastCode {

		public TBoxCode(Ty ret, TCode inner) {
			super(ret, null, inner);
		}

	}

	public static class TUnboxCode extends TCastCode {

		public TUnboxCode(Ty ret, TCode inner) {
			super(ret, null, inner);
		}

	}

}

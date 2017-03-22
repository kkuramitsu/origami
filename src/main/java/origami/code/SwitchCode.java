package origami.code;

import origami.OEnv;
import origami.asm.OAsm;

/**
 * <pre>
 * Params 0 : Condition (OCode) 1 : Case Clauses(CaseCode[])
 **/
public class SwitchCode extends StmtCode {

	public SwitchCode(OEnv env, OCode... nodes) {
		super(env, "switch", nodes);
	}

	@Override
	public void generate(OGenerator gen) {
		gen.pushSwitch(this);
	}

	public OCode condition() {
		return nodes[0];
	}

	public OCode[] caseCode() {
		if (nodes[1] != null) {
			return nodes[1].getParams();
		}
		return null;
	}

	/**
	 * <pre>
	 * Params 0 : Condition (OCode) 1 : Case Clause(MultiCode)
	 **/
	public static class CaseCode extends StmtCode {

		public Object value;

		public CaseCode(OEnv env, Object value, OCode... nodes) {
			super(env, "case", nodes);
			this.value = value;
		}

		public OCode condition() {
			return nodes[0];
		}

		public OCode caseClause() {
			return nodes[1];
		}

	}
}

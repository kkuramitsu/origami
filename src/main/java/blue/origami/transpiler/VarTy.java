package blue.origami.transpiler;

import blue.origami.util.ODebug;
import blue.origami.util.StringCombinator;

public class VarTy extends Ty {
	private String varName;
	private Ty innerTy;

	public VarTy(String varName) {
		this.varName = varName;
		this.innerTy = null;
	}

	public String getName() {
		return this.varName;
	}

	@Override
	public boolean isVar() {
		return this.innerTy == null || this.innerTy.isVar();
	}

	@Override
	public boolean isOption() {
		return this.innerTy == null ? false : this.innerTy.isOption();
	}

	@Override
	public Ty dupTy(VarDomain dom) {
		return this.innerTy == null ? dom.newVarType(this.varName) : this.innerTy.dupTy(dom);
	}

	@Override
	public Ty realTy() {
		return this.innerTy == null ? this.tUntyped : this.innerTy.realTy();
	}

	@Override
	public boolean acceptTy(Ty t) {
		// if (this == t || this == this.realTy()) {
		// return true;
		// }
		if (this.innerTy == null) {
			ODebug.trace("infer %s as %s", this.varName, t);
			this.innerTy = t;
			return true;
		}
		return this.innerTy.acceptTy(t);
	}

	@Override
	public void strOut(StringBuilder sb) {
		if (this.innerTy == null) {
			sb.append(this.varName);
		} else {
			StringCombinator.append(sb, this.innerTy);
		}
	}

	@Override
	public String strOut(TEnv env) {
		return this.innerTy.realTy().strOut(env);
	}

	@Override
	public boolean isUntyped() {
		return this.innerTy == null || this.innerTy.isUntyped();
	}

}
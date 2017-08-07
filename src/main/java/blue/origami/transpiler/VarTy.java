package blue.origami.transpiler;

import blue.origami.util.ODebug;
import blue.origami.util.StringCombinator;

public class VarTy extends Ty {
	private String varName;
	private Ty innerTy;

	public VarTy(String varName) {
		this.varName = varName;
		this.innerTy = Ty.tUntyped;
	}

	public String getName() {
		return this.varName;
	}

	@Override
	public boolean isVar() {
		return this.innerTy.isUntyped();
	}

	@Override
	public boolean isOption() {
		return this.innerTy.isOption();
	}

	@Override
	public Ty dupTy(VarDomain dom) {
		return this.innerTy.isUntyped() ? dom.newVarType(this.varName) : this.innerTy.dupTy(dom);
	}

	@Override
	public Ty realTy() {
		return this.innerTy.realTy();
	}

	@Override
	public boolean acceptTy(Ty t) {
		if (this == t || this == this.realTy()) {
			return true;
		}
		if (this.innerTy.isUntyped()) {
			ODebug.trace("infer %s as %s", this.varName, t);
			this.innerTy = t;
			return true;
		}
		return this.innerTy.acceptTy(t);
	}

	@Override
	public void strOut(StringBuilder sb) {
		if (this.innerTy.isUntyped()) {
			sb.append(this.varName);
		} else {
			StringCombinator.append(sb, this.innerTy);
		}
	}

	@Override
	public String strOut(TEnv env) {
		return this.innerTy.strOut(env);
	}

	@Override
	public boolean isUntyped() {
		return this.innerTy.isUntyped();
	}

}
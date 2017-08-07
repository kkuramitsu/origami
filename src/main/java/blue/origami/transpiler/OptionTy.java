package blue.origami.transpiler;

import blue.origami.util.StringCombinator;

public class OptionTy extends Ty {
	private Ty innerTy;

	public OptionTy(Ty ty) {
		this.innerTy = ty;
	}

	@Override
	public boolean isOption() {
		return true;
	}

	@Override
	public boolean isVar() {
		return this.innerTy.isUntyped();
	}

	@Override
	public Ty dupTy(VarDomain dom) {
		Ty inner = this.innerTy.dupTy(dom);
		if (inner != this.innerTy) {
			return new OptionTy(inner);
		}
		return this;
	}

	@Override
	public Ty realTy() {
		return this;
	}

	@Override
	public boolean acceptTy(Ty t) {
		if (t instanceof OptionTy) {
			return this.innerTy.acceptTy(((OptionTy) t).innerTy);
		}
		return this.innerTy.acceptTy(t);
	}

	@Override
	public String strOut(TEnv env) {
		return this.innerTy.strOut(env);
	}

	@Override
	public boolean isUntyped() {
		return this.innerTy.isUntyped();
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("Option[");
		StringCombinator.append(sb, this.innerTy);
		sb.append("]");
	}

}
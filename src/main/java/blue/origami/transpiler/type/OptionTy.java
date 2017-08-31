package blue.origami.transpiler.type;

import blue.origami.util.StringCombinator;

public class OptionTy extends MonadTy {

	public OptionTy(String name, Ty ty) {
		super(name, ty);
		this.innerTy = ty;
		assert !(ty instanceof OptionTy);
	}

	@Override
	public boolean isOption() {
		return true;
	}

	@Override
	public boolean hasVar() {
		return this.innerTy.hasVar();
	}

	@Override
	public Ty dupVarType(VarDomain dom) {
		Ty inner = this.innerTy.dupVarType(dom);
		if (inner != this.innerTy) {
			return Ty.tOption(inner);
		}
		return this;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		if (codeTy instanceof OptionTy) {
			return this.innerTy.acceptTy(sub, ((OptionTy) codeTy).innerTy, logs);
		}
		if (codeTy instanceof VarTy) {
			return (codeTy.acceptTy(false, this, logs));
		}
		return false;
		// return this.innerTy.acceptTy(sub, codeTy, logs);
	}

	@Override
	public boolean isDynamic() {
		return this.innerTy.isDynamic();
	}

	@Override
	public Ty staticTy() {
		if (this.innerTy instanceof OptionTy) {
			return this.innerTy.staticTy();
		}
		Ty ty = this.innerTy.staticTy();
		if (this.innerTy != ty) {
			return Ty.tOption(ty);
		}
		return this;
	}

	@Override
	public <C> C mapType(TypeMap<C> codeType) {
		return codeType.mapType("Option", this.innerTy);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("Option[");
		StringCombinator.append(sb, this.innerTy);
		sb.append("]");
	}

}
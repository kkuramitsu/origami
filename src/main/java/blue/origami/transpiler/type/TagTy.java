package blue.origami.transpiler.type;

import blue.origami.util.StringCombinator;

public class TagTy extends Ty {
	protected String[] names;
	protected Ty innerTy;

	public TagTy(Ty ty, String... names) {
		this.names = names;
		this.innerTy = ty;
	}

	@Override
	public Ty getInnerTy() {
		return this.innerTy;
	}

	@Override
	public boolean isMutable() {
		return this.innerTy.isMutable();
	}

	@Override
	public boolean hasVar() {
		return this.innerTy.hasVar();
	}

	@Override
	public Ty dupVarType(VarDomain dom) {
		Ty inner = this.innerTy.dupVarType(dom);
		if (inner != this.innerTy) {
			return Ty.tTag(inner, this.names);
		}
		return this;
	}

	@Override
	public boolean isDynamic() {
		return this.innerTy.isDynamic();
	}

	@Override
	public Ty staticTy() {
		Ty ty = this.innerTy.staticTy();
		if (this.innerTy != ty) {
			return Ty.tTag(ty, this.names);
		}
		return this;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		if (codeTy instanceof TagTy && this.innerTy.acceptTy(sub, codeTy.getInnerTy(), logs)) {
			return this.matchTags(sub, ((TagTy) codeTy).names);
		}
		if (codeTy instanceof VarTy) {
			return (codeTy.acceptTy(false, this, logs));
		}
		return false;
	}

	public boolean matchTags(boolean sub, String[] names) {
		if (this.names.length != names.length) {
			return false;
		}
		for (int i = 0; i < names.length; i++) {
			if (!this.names[i].equals(names[i])) {
				return false;
			}
		}
		return true;
	}
	//
	// @Override
	// public int costMapTo(TEnv env, Ty toTy) {
	// if(toTy.acceptTy(false, this.innerTy, logs)) {
	// return
	// }
	// return CastCode.STUPID;
	// }
	//
	// @Override
	// public Template findMapTo(TEnv env, Ty toTy) {
	// return null;
	// }

	// @Override
	// public String key() {
	// return this.name + "[" + this.innerTy.key() + "]";
	// }

	@Override
	public <C> C mapType(TypeMap<C> codeType) {
		return this.innerTy.mapType(codeType);
	}

	@Override
	public void strOut(StringBuilder sb) {
		StringCombinator.append(sb, this.innerTy);
		sb.append(" #");
		StringCombinator.joins(sb, this.names, " #");
	}

}

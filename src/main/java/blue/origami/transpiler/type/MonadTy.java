package blue.origami.transpiler.type;

import blue.origami.util.OStrings;

public class MonadTy extends Ty {
	protected String name;
	protected Ty innerTy;
	protected boolean isMutable;

	public MonadTy(String name, boolean isMutable, Ty ty) {
		this.name = name;
		this.innerTy = ty;
		this.isMutable = isMutable;
		assert (!name.endsWith("'"));
	}

	public MonadTy(String name, Ty ty) {
		this(name, false, ty);
	}

	@Override
	public boolean isNonMemo() {
		return this.innerTy.isNonMemo();
	}

	public Ty newType(String name, Ty ty) {
		return new MonadTy(name, this.isMutable, ty);
	}

	public boolean equalsName(String name) {
		return this.name.equals(name);
	}

	@Override
	public boolean isMutable() {
		return this.isMutable || this.getInnerTy().isMutable();
	}

	@Override
	public Ty toImmutable() {
		return Ty.tMonad(this.name, this.getInnerTy().toImmutable());
	}

	@Override
	public Ty getInnerTy() {
		return this.innerTy;
	}

	@Override
	public boolean hasVar() {
		return this.innerTy.hasVar();
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		Ty inner = this.innerTy.dupVar(dom);
		if (inner != this.innerTy) {
			return Ty.tMonad(this.name, this.isMutable, inner);
		}
		return this;
	}

	@Override
	public Ty finalTy() {
		Ty ty = this.innerTy.finalTy();
		if (this.innerTy != ty) {
			return Ty.tMonad(this.name, this.isMutable, ty);
		}
		return this;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		if (codeTy.isMonad(this.name)) {
			return this.innerTy.acceptTy(false, codeTy.getInnerTy(), logs);
		}
		return this.acceptVarTy(sub, codeTy, logs);
	}

	@Override
	public <C> C mapType(TypeMap<C> codeType) {
		return codeType.mapType(this.name, this.innerTy);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.name);
		sb.append(this.isMutable ? "{" : "[");
		OStrings.append(sb, this.innerTy);
		sb.append(this.isMutable ? "}" : "]");
	}

}

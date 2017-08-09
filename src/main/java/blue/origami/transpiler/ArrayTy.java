package blue.origami.transpiler;

import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataArrayCode;
import blue.origami.util.StringCombinator;

public class ArrayTy extends MonadTy {

	public ArrayTy(Ty innerType) {
		super(innerType);
	}

	public ArrayTy asImmutable() {
		this.isImmutable = true;
		return this;
	}

	@Override
	public Code getDefaultValue() {
		return new DataArrayCode(this.isImmutable ? Ty.tImArray(this.innerType) : Ty.tArray(this.innerType));
	}

	@Override
	public void strOut(StringBuilder sb) {
		if (this.isImmutable) {
			sb.append("{");
		}
		StringCombinator.append(sb, this.innerType);
		sb.append("*");
		if (this.isImmutable) {
			sb.append("}");
		}
	}

	@Override
	public Ty dupTy(VarDomain dom) {
		Ty ty = this.innerType.dupTy(dom);
		if (ty != this.innerType) {
			return this.isImmutable ? Ty.tImArray(ty) : Ty.tArray(ty);
		}
		return this;

	}

	@Override
	public boolean acceptTy(boolean sub, Ty t, boolean updated) {
		if (t instanceof VarTy) {
			return (t.acceptTy(false, this, updated));
		}
		if (t instanceof ArrayTy) {
			return this.innerType.acceptTy(false, t.getInnerTy(), updated);
		}
		return false;
	}

	@Override
	public Ty nomTy() {
		Ty ty = this.innerType.nomTy();
		if (ty != this.innerType) {
			return this.isImmutable ? Ty.tImArray(ty) : Ty.tArray(ty);
		}
		return this;
	}

}
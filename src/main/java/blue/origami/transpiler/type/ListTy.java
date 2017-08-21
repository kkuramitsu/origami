package blue.origami.transpiler.type;

import blue.origami.transpiler.VarDomain;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataListCode;
import blue.origami.util.StringCombinator;

public class ListTy extends MutableTy {

	public ListTy(Ty innerType) {
		super(innerType);
		assert (innerType != null);
	}

	public ListTy asImmutable() {
		this.isImmutable = true;
		return this;
	}

	@Override
	public Code getDefaultValue() {
		return new DataListCode(this.isImmutable ? Ty.tImList(this.innerType) : Ty.tList(this.innerType));
	}

	@Override
	public void strOut(StringBuilder sb) {
		StringCombinator.append(sb, this.innerType);
		sb.append(this.isImmutable ? "*" : "[]");
	}

	@Override
	public Ty dupTy(VarDomain dom) {
		Ty ty = this.innerType.dupTy(dom);
		if (ty != this.innerType) {
			return this.isImmutable ? Ty.tImList(ty) : Ty.tList(ty);
		}
		return this;

	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, boolean updated) {
		if (codeTy instanceof VarTy) {
			return (codeTy.acceptTy(false, this, updated));
		}
		if (codeTy instanceof ListTy) {
			return this.innerType.acceptTy(false, codeTy.getInnerTy(), updated);
		}
		return false;
	}

	@Override
	public Ty nomTy() {
		Ty ty = this.innerType.nomTy();
		if (ty != this.innerType) {
			return this.isImmutable ? Ty.tImList(ty) : Ty.tList(ty);
		}
		return this;
	}

	@Override
	public <C> C mapType(TypeMap<C> codeType) {
		return codeType.mapType(this.isImmutable ? "List" : "List'", this.getInnerTy());
	}

}
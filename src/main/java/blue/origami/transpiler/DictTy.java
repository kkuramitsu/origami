package blue.origami.transpiler;

import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataDictCode;
import blue.origami.util.StringCombinator;

public class DictTy extends MonadTy {

	DictTy(Ty innerType) {
		super(innerType);
	}

	public DictTy asImmutable() {
		this.isImmutable = true;
		return this;
	}

	@Override
	public Code getDefaultValue() {
		return new DataDictCode(this.isImmutable ? Ty.tImDict(this.innerType) : Ty.tDict(this.innerType));
	}

	@Override
	public void strOut(StringBuilder sb) {
		if (!this.isImmutable) {
			sb.append("$");
		}
		sb.append("Dict[");
		StringCombinator.append(sb, this.innerType);
		sb.append("]");
	}

	@Override
	public Ty dupTy(VarDomain dom) {
		Ty ty = this.innerType.dupTy(dom);
		if (ty != this.innerType) {
			return this.isImmutable ? Ty.tImDict(ty) : Ty.tDict(ty);
		}
		return this;

	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, boolean updated) {
		if (codeTy instanceof VarTy) {
			return (codeTy.acceptTy(false, this, updated));
		}
		if (codeTy instanceof DictTy) {
			return this.innerType.acceptTy(false, codeTy.getInnerTy(), updated);
		}
		return false;
	}

	@Override
	public Ty nomTy() {
		Ty ty = this.innerType.nomTy();
		if (ty != this.innerType) {
			return this.isImmutable ? Ty.tImDict(ty) : Ty.tDict(ty);
		}
		return this;
	}

}
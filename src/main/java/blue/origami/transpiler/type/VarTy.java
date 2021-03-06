package blue.origami.transpiler.type;

import java.util.function.Function;
import java.util.function.Predicate;

import blue.origami.transpiler.Env;
import origami.nez2.OStrings;
import origami.nez2.Token;

public class VarTy extends Ty {
	private static int seq = 27;
	Ty varref;
	final int varId;

	VarTy(Token name) {
		this.varref = null;
		this.varId = seq++;
		assert (seq > 0);
	}

	@Override
	public boolean eq(Ty ty) {
		if (this == ty) {
			return true;
		}
		if (this.varref != null) {
			return this.varref.eq(ty);
		}
		return false;
	}

	private String name() {
		return ""; // this.name1 == null ? "" : this.name1.getString();
	}

	public String getId() {
		return this.name() + Ty.NonMemoChar + this.varId;
	}

	@Override
	public String keyOfMemo() {
		return this.varref == null ? this.getId() : this.varref.keyOfMemo();
	}

	@Override
	public String keyOfArrows() {
		return this.varref == null ? "a" : this.varref.keyOfArrows();
	}

	@Override
	public Ty newGeneric(Ty paramTy) {
		return this.varref == null ? new GenericTy(this, paramTy) : this.varref.newGeneric(paramTy);
	}

	@Override
	public Ty getParamType() {
		return this.varref == null ? this : this.varref.getParamType();
	}

	private boolean enforceMutable = false;
	private boolean enforceImmutable = false;

	@Override
	public boolean isMutable() {
		return this.varref == null ? this.enforceMutable : this.varref.isMutable();
	}

	@Override
	public Ty toMutable() {
		if (!this.enforceImmutable) {
			if (this.varref != null) {
				this.varref = this.varref.toMutable();
				return this;
			}
			this.enforceMutable = true;
		}
		return this;
	}

	@Override
	public Ty toImmutable() {
		if (!this.enforceMutable) {
			if (this.varref != null) {
				this.varref = this.varref.toImmutable();
			}
			this.enforceImmutable = true;
		}
		return this;
	}

	@Override
	public boolean hasSome(Predicate<Ty> f) {
		return this.isVar() || this.varref == null || this.varref.hasSome(f);
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		return this.varref == null ? dom.convToParam(this) : this.varref.dupVar(dom);
	}

	@Override
	public Ty map(Function<Ty, Ty> f) {
		Ty self = f.apply(this);
		if (self != this) {
			return self;
		}
		return this.varref == null ? this : this.varref.map(f);
	}

	@Override
	public Ty devar() {
		return this.varref == null ? this : this.varref.devar();
	}

	@Override
	public Ty memoed() {
		return (this.varref == null) ? this : this.varref.memoed();
	}

	private boolean lt(VarTy vt) {
		return this.varId > vt.varId;
	}

	boolean matchVar(TypeMatchContext tmx, Ty left) {
		assert this.varref == null;
		left = left.inferType(tmx);
		if (this.enforceMutable) {
			left = left.toMutable();
		}

		// if (this.varref != null) {
		// return this.varref.match(tmx, sub, right);
		// }
		if (left.isVar()) {
			VarTy varTy = (VarTy) left.devar();
			// if (varTy.varref != null) {
			// return this.match(tmx, sub, varTy.varref);
			// }
			if (this.varId != varTy.varId) {
				return this.lt(varTy) ? tmx.updateVar(varTy, this) : tmx.updateVar(this, varTy);
			}
			return true;
		}
		tmx.updateVar(this, left);
		return true;
	}

	@Override
	public void strOut(StringBuilder sb) {
		if (this.varref == null) {
			sb.append(this.getId());
		} else {
			sb.append(this.getId());
			sb.append("=");
			OStrings.append(sb, this.varref);
		}
	}

	@Override
	public void typeKey(StringBuilder sb) {
		if (this.varref == null) {
			sb.append("a");
		} else {
			this.varref.typeKey(sb);
		}
	}

	@Override
	public Ty resolveFieldType(Env env, Token s, String name) {
		if (this.varref == null) {
			this.varref = new DataVarTy();
		}
		return this.varref.resolveFieldType(env, s, name);
	}

	@Override
	public <C> C mapType(TypeMapper<C> codeType) {
		if (this.varref == null) {
			return codeType.mapType("a");
		} else {
			return this.varref.mapType(codeType);
		}
	}

}
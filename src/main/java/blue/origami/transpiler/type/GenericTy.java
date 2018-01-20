package blue.origami.transpiler.type;

import java.util.function.Function;
import java.util.function.Predicate;

import blue.origami.common.OStrings;
import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.Env;

public class GenericTy extends Ty {
	protected Ty base;
	protected Ty param;

	public GenericTy(Ty baseTy, Ty paramTy) {
		this.base = baseTy;
		this.param = paramTy;
	}

	@Override
	public boolean eq(Ty ty) {
		Ty right = ty.devar();
		if (this == right) {
			return true;
		}
		if (right instanceof GenericTy) {
			GenericTy gt = (GenericTy) right;
			return this.base.eq(gt.base) && this.param.eq(gt.param);
		}
		return false;
	}

	@Override
	public int paramSize() {
		return 1;
	}

	@Override
	public Ty param(int n) {
		if (n == 0) {
			return this.param;
		}
		return null;
	}

	@Override
	public Ty[] params() {
		return new Ty[] { this.param };
	}

	@Override
	public Ty newGeneric(Ty paramTy) {
		return new GenericTy(this.base, paramTy);
	}

	public Ty getBaseType() {
		return this.base;
	}

	@Override
	public Ty getParamType() {
		return this.param;
	}

	@Override
	public boolean hasSome(Predicate<Ty> f) {
		return this.base.hasSome(f) || this.param.hasSome(f);
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		Ty base = this.base.dupVar(dom);
		Ty inner = this.param.dupVar(dom);
		if (inner != this.param || base != this.base) {
			return Ty.tGeneric(base, inner);
		}
		return this;
	}

	@Override
	public Ty map(Function<Ty, Ty> f) {
		Ty self = f.apply(this);
		if (self != this) {
			return self;
		}
		Ty base = this.base.map(f);
		Ty inner = this.param.map(f);
		if (inner != this.param || base != this.base) {
			return Ty.tGeneric(base, inner);
		}
		return this;
	}

	// @Override
	// public Ty devar() {
	// if (this.hasSome(Ty.IsVar)) {
	// Ty base = this.base.devar();
	// Ty inner = this.param.devar();
	// if (this.param != inner || this.base != base) {
	// return Ty.tGeneric(base, inner);
	// }
	// }
	// return this;
	// }

	@Override
	public Ty memoed() {
		if (!this.isMemoed()) {
			return Ty.tGeneric(this.base.memoed(), this.param.memoed());
		}
		return this;
	}

	@Override
	public boolean hasSuperType(Ty left) {
		if (this.eq(left)) {
			return true;
		}
		if (left.isGeneric()) {
			GenericTy lt = (GenericTy) left.devar();
			return this.base.hasSuperType(lt.base);
		}
		return false;
	}

	@Override
	public <C> C mapType(TypeMapper<C> codeType) {
		return codeType.mapType(this.base.keyOfMemo(), this.param);
	}

	@Override
	public void strOut(StringBuilder sb) {
		OStrings.append(sb, this.base);
		sb.append("[");
		OStrings.append(sb, this.param);
		sb.append("]");
	}

	@Override
	public void typeKey(StringBuilder sb) {
		this.base.typeKey(sb);
		sb.append("[");
		this.param.typeKey(sb);
		sb.append("]");
	}

	@Override
	public String keyOfArrows() {
		return this.base.keyOfArrows();
	}

	@Override
	public int costMapThisTo(Env env, Ty fromTy, Ty toTy) {
		assert (this == fromTy);
		return this.base.costMapThisTo(env, this, toTy);
	}

	@Override
	public CodeMap findMapThisTo(Env env, Ty fromTy, Ty toTy) {
		assert (this == fromTy);
		return this.base.findMapThisTo(env, this, toTy);
	}

	@Override
	public int costMapFromToThis(Env env, Ty fromTy, Ty toTy) {
		assert (this == toTy);
		return this.base.costMapFromToThis(env, fromTy, this);
	}

	@Override
	public CodeMap findMapFromToThis(Env env, Ty fromTy, Ty toTy) {
		assert (this == toTy);
		return this.base.findMapFromToThis(env, fromTy, this);
	}

}

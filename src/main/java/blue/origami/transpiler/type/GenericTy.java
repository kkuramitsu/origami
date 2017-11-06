package blue.origami.transpiler.type;

import java.util.function.Function;
import java.util.function.Predicate;

import blue.origami.common.OStrings;
import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.Env;

public class GenericTy extends Ty {
	protected Ty baseTy;
	protected Ty paramTy;

	public GenericTy(Ty baseTy, Ty paramTy) {
		this.baseTy = baseTy;
		this.paramTy = paramTy;
	}

	@Override
	public Ty newGeneric(Ty paramTy) {
		return new GenericTy(this.baseTy, paramTy);
	}

	public Ty getBaseType() {
		return this.baseTy;
	}

	@Override
	public Ty getParamType() {
		return this.paramTy;
	}

	@Override
	public boolean isMutable() {
		return this.baseTy.isMutable() || this.paramTy.isMutable();
	}

	@Override
	public Ty toImmutable() {
		if (this.isMutable()) {
			return Ty.tGeneric(this.baseTy.toImmutable(), this.getParamType().toImmutable());
		}
		return this;
	}

	@Override
	public boolean hasSome(Predicate<Ty> f) {
		return this.baseTy.hasSome(f) || this.paramTy.hasSome(f);
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		Ty base = this.baseTy.dupVar(dom);
		Ty inner = this.paramTy.dupVar(dom);
		if (inner != this.paramTy || base != this.baseTy) {
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
		Ty base = this.baseTy.map(f);
		Ty inner = this.paramTy.map(f);
		if (inner != this.paramTy || base != this.baseTy) {
			return Ty.tGeneric(base, inner);
		}
		return this;
	}

	@Override
	public Ty base() {
		if (this.hasSome(Ty.IsVar)) {
			Ty base = this.baseTy.base();
			Ty inner = this.paramTy.base();
			if (this.paramTy != inner || this.baseTy != base) {
				return Ty.tGeneric(base, inner);
			}
		}
		return this;
	}

	@Override
	public Ty memoed() {
		if (!this.isMemoed()) {
			return Ty.tGeneric(this.baseTy.memoed(), this.paramTy.memoed());
		}
		return this;
	}

	@Override
	public boolean match(boolean sub, Ty codeTy, TypeMatcher logs) {
		if (codeTy.isGeneric()) {
			GenericTy gt = (GenericTy) codeTy.base();
			return this.baseTy.match(false, gt.getBaseType(), logs)
					&& this.paramTy.match(false, gt.getParamType(), logs);
		}
		return this.matchVar(sub, codeTy, logs);
	}

	@Override
	public <C> C mapType(TypeMapper<C> codeType) {
		return codeType.mapType(this.baseTy.keyMemo(), this.paramTy);
	}

	@Override
	public void strOut(StringBuilder sb) {
		OStrings.append(sb, this.baseTy);
		sb.append("[");
		OStrings.append(sb, this.paramTy);
		sb.append("]");
	}

	@Override
	public void typeKey(StringBuilder sb) {
		this.baseTy.typeKey(sb);
		sb.append("[");
		this.paramTy.typeKey(sb);
		sb.append("]");
	}

	@Override
	public String keyFrom() {
		return this.baseTy.keyFrom();
	}

	@Override
	public int costMapThisTo(Env env, Ty fromTy, Ty toTy) {
		assert (this == fromTy);
		return this.baseTy.costMapThisTo(env, this, toTy);
	}

	@Override
	public CodeMap findMapThisTo(Env env, Ty fromTy, Ty toTy) {
		assert (this == fromTy);
		return this.baseTy.findMapThisTo(env, this, toTy);
	}

	@Override
	public int costMapFromToThis(Env env, Ty fromTy, Ty toTy) {
		assert (this == toTy);
		return this.baseTy.costMapFromToThis(env, fromTy, this);
	}

	@Override
	public CodeMap findMapFromToThis(Env env, Ty fromTy, Ty toTy) {
		assert (this == toTy);
		return this.baseTy.findMapFromToThis(env, fromTy, this);
	}

}

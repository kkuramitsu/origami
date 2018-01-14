package blue.origami.transpiler.type;

import java.util.function.Function;
import java.util.function.Predicate;

import blue.origami.common.OStrings;

public class CondTy extends Ty {
	protected Ty baseTy;
	protected boolean pred;
	protected Ty condTy;

	public CondTy(Ty baseTy, boolean pred, Ty paramTy) {
		this.baseTy = baseTy;
		this.pred = pred;
		this.condTy = paramTy;
	}

	public Ty getBaseType() {
		return this.baseTy;
	}

	@Override
	public boolean isMutable() {
		return this.baseTy.isMutable();
	}

	@Override
	public Ty toMutable() {
		if (!this.isMutable()) {
			return Ty.tCond(this.baseTy.toMutable(), this.pred, this.condTy);
		}
		return this;
	}

	@Override
	public Ty toImmutable() {
		if (this.isMutable()) {
			return Ty.tCond(this.baseTy.toImmutable(), this.pred, this.condTy);
		}
		return this;
	}

	@Override
	public boolean hasSome(Predicate<Ty> f) {
		return this.baseTy.hasSome(f);
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		Ty base = this.baseTy.dupVar(dom);
		if (base != this.baseTy) {
			return Ty.tCond(base, this.pred, this.condTy);
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
		if (base != this.baseTy) {
			return Ty.tCond(base, this.pred, this.condTy);
		}
		return this;
	}

	@Override
	public Ty base() {
		Ty base = this.baseTy.base();
		if (base != this.baseTy) {
			return Ty.tCond(base, this.pred, this.condTy);
		}
		return this;
	}

	@Override
	public Ty memoed() {
		if (!this.isMemoed()) {
			return Ty.tCond(this.baseTy.memoed(), this.pred, this.condTy.memoed());
		}
		return this;
	}

	@Override
	public boolean match(boolean sub, Ty codeTy, TypeMatcher logs) {
		if (this.pred) {
			return this.condTy.match(sub, codeTy, logs) && this.baseTy.match(sub, codeTy, logs);
		}
		return !this.condTy.match(sub, codeTy, logs) && this.baseTy.match(sub, codeTy, logs);
	}

	@Override
	public <C> C mapType(TypeMapper<C> codeType) {
		return codeType.mapType(this.baseTy.keyMemo());
	}

	@Override
	public void strOut(StringBuilder sb) {
		OStrings.append(sb, this.baseTy);
		sb.append(this.pred ? " &" : " !");
		OStrings.append(sb, this.condTy);
	}

	@Override
	public void typeKey(StringBuilder sb) {
		this.baseTy.typeKey(sb);
		sb.append(this.pred ? "&" : "!");
		this.condTy.typeKey(sb);
	}

	@Override
	public String keyFrom() {
		return this.baseTy.keyFrom();
	}

}

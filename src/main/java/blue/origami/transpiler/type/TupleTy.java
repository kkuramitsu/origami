package blue.origami.transpiler.type;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

import blue.origami.common.OArrays;
import origami.nez2.OStrings;

public class TupleTy extends Ty {
	protected final Ty[] paramTypes;

	TupleTy(Ty... paramTypes) {
		this.paramTypes = paramTypes;
		assert (this.paramTypes.length > 1) : "tuple size " + this.paramTypes.length;
	}

	@Override
	public boolean eq(Ty ty) {
		Ty right = ty.devar();
		if (this == right) {
			return true;
		}
		if (right instanceof TupleTy) {
			TupleTy dt = (TupleTy) right;
			for (int i = 0; i < dt.paramSize(); i++) {
				if (!dt.param(i).eq(this.param(i))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public int paramSize() {
		return this.paramTypes.length;
	}

	@Override
	public Ty param(int n) {
		return this.paramTypes[n];
	}

	@Override
	public Ty[] params() {
		return this.paramTypes;
	}

	public int getParamSize() {
		return this.paramTypes.length;
	}

	public Ty[] getParamTypes() {
		return this.paramTypes;
	}

	@Override
	public void strOut(StringBuilder sb) {
		OStrings.joins(sb, this.paramTypes, "*");
	}

	@Override
	public void typeKey(StringBuilder sb) {
		Ty[] ts = this.paramTypes;
		OStrings.forEach(sb, ts.length, "*", (n) -> ts[n].typeKey(sb));
	}

	@Override
	public String keyOfArrows() {
		return "Tuple" + this.paramTypes.length;
	}

	@Override
	public boolean hasSome(Predicate<Ty> f) {
		return OArrays.testSome(this.getParamTypes(), t -> t.hasSome(f));
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		if (this.hasSome(Ty.IsVarParam)) {
			return Ty.tTuple(Ty.map(this.paramTypes, x -> x.dupVar(dom)));
		}
		return this;
	}

	@Override
	public Ty map(Function<Ty, Ty> f) {
		Ty self = f.apply(this);
		if (self != this) {
			return self;
		}
		Ty[] ts = Ty.map(this.paramTypes, x -> x.map(f));
		if (Arrays.equals(ts, this.paramTypes)) {
			return this;
		}
		return Ty.tTuple(ts);
	}

	@Override
	public boolean matchBase(boolean sub, Ty right) {
		return right.isTuple() && this.paramSize() <= right.paramSize();
	}

	@Override
	public boolean hasSuperType(Ty left) {
		return left == this;
	}

	@Override
	public Ty memoed() {
		if (!this.isMemoed()) {
			return Ty.tTuple(Ty.map(this.paramTypes, t -> t.memoed()));
		}
		return this;
	}

	@Override
	public <C> C mapType(TypeMapper<C> codeType) {
		return codeType.forTupleType(this);
	}

}

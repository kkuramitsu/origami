package blue.origami.transpiler.type;

import java.util.Arrays;
import java.util.function.Predicate;

import blue.origami.common.OArrays;
import blue.origami.common.OStrings;

public class TupleTy extends Ty {
	protected final Ty[] paramTypes;

	TupleTy(Ty... paramTypes) {
		this.paramTypes = paramTypes;
		assert (this.paramTypes.length > 1) : "tuple size " + this.paramTypes.length;
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
	public boolean hasSome(Predicate<Ty> f) {
		return OArrays.testSomeTrue(t -> t.hasSome(f), this.getParamTypes());
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		if (this.hasSome(Ty.IsVarParam)) {
			return Ty.tTuple(Arrays.stream(this.paramTypes).map(x -> x.dupVar(dom)).toArray(Ty[]::new));
		}
		return this;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		if (codeTy.isTuple()) {
			TupleTy tupleTy = (TupleTy) codeTy.base();
			if (tupleTy.getParamSize() != this.getParamSize()) {
				return false;
			}
			for (int i = 0; i < this.getParamSize(); i++) {
				if (!this.paramTypes[i].acceptTy(false, tupleTy.paramTypes[i], logs)) {
					return false;
				}
			}
			return true;
		}
		return this.acceptVarTy(sub, codeTy, logs);
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

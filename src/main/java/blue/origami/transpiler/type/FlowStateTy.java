package blue.origami.transpiler.type;

public class FlowStateTy extends Ty {
	private Ty innerTy;
	private boolean hasMutation = false;

	FlowStateTy(Ty innerTy) {
		this.innerTy = innerTy;
		assert (innerTy.isMutable());
	}

	@Override
	public boolean isNonMemo() {
		return true;
	}

	@Override
	public boolean hasMutation() {
		return this.hasMutation;
	}

	@Override
	public void hasMutation(boolean b) {
		this.hasMutation = b;
	}

	@Override
	public Ty finalTy() {
		if (!this.hasMutation) {
			return this.innerTy.toImmutable();
		}
		return this.innerTy;
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.innerTy.strOut(sb);
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		return this.innerTy.acceptTy(sub, codeTy, logs);
	}

	@Override
	public boolean hasVar() {
		return this.innerTy.hasVar();
	}

	@Override
	public <C> C mapType(TypeMap<C> codeType) {
		return this.innerTy.mapType(codeType);
	}

	public static Ty flowTy(Ty ty) {
		if (ty.isData()) {
			return new FlowDataTy();
		}
		if (ty.isList()) {
			return new FlowStateTy(Ty.tMonad("List'", ty.real().getInnerTy()));
		}
		if (ty.isDict()) {
			return new FlowStateTy(Ty.tMonad("Dict'", ty.real().getInnerTy()));
		}
		return ty;
	}

}

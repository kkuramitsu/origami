package blue.origami.transpiler.type;

public class FlowDataTy extends DataTy {

	public FlowDataTy() {
		super();
	}

	@Override
	public boolean isNonMemo() {
		return true;
	}

	private boolean hasMutation = false;

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
			return Ty.tRecord(this.names());
		}
		return Ty.tData(this.names());
	}

	@Override
	public boolean hasField(String field, VarLogger logs) {
		if (!super.hasField(field, logs)) {
			if (logs.isUpdate()) {
				this.fields.add(field);
			}
		}
		return true;
	}

}
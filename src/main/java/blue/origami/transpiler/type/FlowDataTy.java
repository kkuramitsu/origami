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
	public void hasMutation(boolean b) {
		this.hasMutation = b;
	}

	@Override
	public boolean hasField(String field, VarLogger logs) {
		if (!this.hasField(field)) {
			if (logs.isUpdate()) {
				this.fields.add(field);
			}
			return true;
		}
		return true;
	}
}
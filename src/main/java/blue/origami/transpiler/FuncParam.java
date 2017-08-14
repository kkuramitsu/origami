package blue.origami.transpiler;

public interface FuncParam {

	public String[] getParamNames();

	public Ty[] getParamTypes();

	public default int getStartIndex() {
		return 0;
	}

	public default String getNameAt(int index) {
		if (getParamNames().length == 0) {
			return String.valueOf((char) ('a' + index));
		}
		return getParamNames()[index] + (this.getStartIndex() + index);
	}

	public default int size() {
		return this.getParamTypes().length;
	}
}

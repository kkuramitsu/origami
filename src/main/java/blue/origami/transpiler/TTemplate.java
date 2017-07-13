package blue.origami.transpiler;

public abstract class TTemplate {
	public final static TTemplate Null = null;

	protected final String name;
	protected final TType[] paramTypes;
	protected final TType returnType;
	// private final String template;

	public TTemplate(String name, TType returnType, TType... paramTypes) {
		this.name = name;
		this.paramTypes = paramTypes;
		this.returnType = returnType;
	}

	public String getName() {
		return this.name;
	}

	public TType getReturnType() {
		return this.returnType;
	}

	public int getParamSize() {
		return this.paramTypes.length;
	}

	public TType[] getParamTypes() {
		return this.paramTypes;
	}

	public abstract String format(Object... args);

	public TInst[] getInsts() {
		return TConsts.emptyInsts;
	}

}

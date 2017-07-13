package blue.origami.transpiler;

public abstract class TTemplate {
	public final static TTemplate Null = null;

	private final String name;
	private final TType[] paramTypes;
	private final TType returnType;
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

class TCodeTemplate extends TTemplate {

	private String template = null;

	TCodeTemplate(String name, TType returnType, TType[] paramTypes, String template) {
		super(name, returnType, paramTypes);
		this.template = template;
	}

	@Override
	public String format(Object... args) {
		return String.format(this.template, args);
	}

	@Override
	public TInst[] getInsts() {
		return TConsts.emptyInsts;
	}

}

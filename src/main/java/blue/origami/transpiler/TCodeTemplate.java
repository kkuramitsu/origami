package blue.origami.transpiler;

public class TCodeTemplate extends TTemplate {

	protected String template = null;

	public TCodeTemplate(String name, TType returnType, TType[] paramTypes, String template) {
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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getName());
		for (TType t : this.getParamTypes()) {
			sb.append(":");
			sb.append(t);
		}
		sb.append(":");
		sb.append(this.getReturnType());
		sb.append("=");
		sb.append(this.template);
		return sb.toString();
	}

}
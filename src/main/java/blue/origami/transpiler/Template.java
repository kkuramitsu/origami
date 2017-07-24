package blue.origami.transpiler;

import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TParamCode;

public abstract class Template {
	public final static Template Null = null;
	// Skeleton
	protected final String name;
	protected final TType[] paramTypes;
	protected final TType returnType;
	// private final String template;

	public Template(String name, TType returnType, TType... paramTypes) {
		this.name = name;
		this.returnType = returnType;
		this.paramTypes = paramTypes;
		assert (this.returnType != null) : this;
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

	public abstract String getDefined();

	public boolean isEnabled() {
		return true;
	}

	public abstract TParamCode newParamCode(TEnv env, String name, TCode[] params);

	public abstract String format(Object... args);

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
		return sb.toString();
	}

}

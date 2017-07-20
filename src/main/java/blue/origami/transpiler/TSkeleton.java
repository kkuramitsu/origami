package blue.origami.transpiler;

import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TParamCode;

public abstract class TSkeleton {
	public final static TSkeleton Null = null;
	// Skeleton
	protected final String name;
	protected final TType[] paramTypes;
	protected final TType returnType;
	// private final String template;

	public TSkeleton(String name, TType returnType, TType... paramTypes) {
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

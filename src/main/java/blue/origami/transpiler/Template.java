package blue.origami.transpiler;

import blue.origami.transpiler.code.TCode;

public abstract class Template {
	public final static Template Null = null;
	// Skeleton
	protected boolean isGeneric;
	protected final String name;
	protected final TType[] paramTypes;
	protected final TType returnType;
	// private final String template;

	public Template(String name, TType returnType, TType... paramTypes) {
		this.name = name;
		this.returnType = returnType;
		this.paramTypes = paramTypes;
		for (TType t : paramTypes) {
			if (t.isVarType()) {
				this.isGeneric = true;
				break;
			}
		}
		assert (this.returnType != null) : this;
	}

	public String getName() {
		return this.name;
	}

	public TType getReturnType() {
		return this.returnType;
	}

	public boolean isGeneric() {
		return this.isGeneric;
	}

	public int getParamSize() {
		return this.paramTypes.length;
	}

	public TType[] getParamTypes() {
		return this.paramTypes;
	}

	public abstract String getDefined();

	public boolean isExpired() {
		return false;
	}

	public Template update(TEnv env, TCode[] params) {
		return this;
	}

	public abstract String format(Object... args);

	public TInst[] getInsts() {
		return TArrays.emptyInsts;
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

	public TFuncType getFuncType() {
		return (TFuncType) TType.tFunc(this.getReturnType(), this.getParamTypes());
	}

}

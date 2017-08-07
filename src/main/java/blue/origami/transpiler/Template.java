package blue.origami.transpiler;

import blue.origami.transpiler.code.Code;

public abstract class Template {
	public final static Template Null = null;
	// Skeleton
	protected boolean isGeneric;
	protected final String name;
	protected final Ty[] paramTypes;
	protected final Ty returnType;
	// private final String template;

	public Template(String name, Ty returnType, Ty... paramTypes) {
		this.name = name;
		this.returnType = returnType;
		this.paramTypes = paramTypes;
		for (Ty t : paramTypes) {
			if (t.isVar()) {
				this.isGeneric = true;
				break;
			}
		}
		assert (this.returnType != null) : this;
	}

	public String getName() {
		return this.name;
	}

	public Ty getReturnType() {
		return this.returnType;
	}

	public boolean isGeneric() {
		return this.isGeneric;
	}

	public int getParamSize() {
		return this.paramTypes.length;
	}

	public Ty[] getParamTypes() {
		return this.paramTypes;
	}

	public abstract String getDefined();

	public boolean isExpired() {
		return false;
	}

	public Template update(TEnv env, Code[] params) {
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
		for (Ty t : this.getParamTypes()) {
			sb.append(":");
			sb.append(t);
		}
		sb.append(":");
		sb.append(this.getReturnType());
		return sb.toString();
	}

	public FuncTy getFuncType() {
		return (FuncTy) Ty.tFunc(this.getReturnType(), this.getParamTypes());
	}

}

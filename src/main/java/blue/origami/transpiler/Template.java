package blue.origami.transpiler;

import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.type.FuncTy;
import blue.origami.transpiler.type.Ty;

public abstract class Template {
	public final static Template Null = null;
	protected boolean isPure;
	protected boolean isFaulty;
	protected short cost = 0;

	// Skeleton
	protected boolean isGeneric;
	protected final String name;
	protected Ty[] paramTypes;
	protected Ty returnType;

	// private final String template;

	public Template(String name, Ty returnType, Ty... paramTypes) {
		this.name = name;
		this.returnType = returnType;
		this.paramTypes = paramTypes;
		this.isGeneric = Ty.hasVar(paramTypes);
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

	public final boolean isPure() {
		return this.isPure;
	}

	public Template asPure(boolean pure) {
		this.isPure = pure;
		return this;
	}

	public final boolean isFaulty() {
		return this.isFaulty;
	}

	public Template asFaulty(boolean faulty) {
		this.isFaulty = faulty;
		return this;
	}

	public int mapCost() {
		return this.cost;
	}

	public Template setMapCost(int cost) {
		this.cost = (short) cost;
		return this;
	}

	public abstract String getDefined();

	public boolean isExpired() {
		return false;
	}

	public Template update(TEnv env, Code[] params) {
		return this;
	}

	public abstract String format(Object... args);

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
		if (!this.isPure()) {
			sb.append("@");
		}
		return sb.toString();
	}

	public FuncTy getFuncType() {
		return Ty.tFunc(this.getReturnType(), this.getParamTypes());
	}

}

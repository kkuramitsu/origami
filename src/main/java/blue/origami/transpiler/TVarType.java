package blue.origami.transpiler;

import blue.origami.util.ODebug;

public class TVarType extends TType {
	private String varName;
	private TType wrappedType;

	public TVarType(String varName) {
		this.varName = varName;
		this.wrappedType = TType.tUntyped;
	}

	public String getName() {
		return this.varName;
	}

	@Override
	public boolean isVarType() {
		return this.wrappedType.isUntyped();
	}

	@Override
	public TType dup(TVarDomain dom) {
		return this.wrappedType.isUntyped() ? dom.newVarType(this.varName) : this.wrappedType.dup(dom);
	}

	@Override
	public TType realType() {
		return this.wrappedType.realType();
	}

	@Override
	public boolean acceptType(TType t) {
		if (this == t || this == this.realType()) {
			return true;
		}
		if (this.wrappedType.isUntyped()) {
			ODebug.trace("infer %s as %s", this.varName, t);
			this.wrappedType = t;
			return true;
		}
		return this.wrappedType.acceptType(t);
	}

	@Override
	public String toString() {
		if (this.wrappedType.isUntyped()) {
			return this.varName;
		} else {
			return this.wrappedType.toString();
		}
	}

	@Override
	public String strOut(TEnv env) {
		return this.wrappedType.strOut(env);
	}

	@Override
	public boolean isUntyped() {
		return this.wrappedType.isUntyped();
	}

	// @Override
	// public boolean accept(TCode code) {
	// if (this.isUntyped()) {
	// TType t = code.guessType();
	// this.setType(t);
	// return true;
	// }
	// return this.wrappedType.accept(code);
	// }

}
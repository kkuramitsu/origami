package blue.origami.transpiler.type;

import blue.origami.transpiler.code.Code;

public class SimpleTy extends Ty {
	private String name;

	public SimpleTy(String name) {
		this.name = name;
	}

	@Override
	public Code getDefaultValue() {
		return null;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		if (codeTy instanceof VarTy) {
			return (codeTy.acceptTy(false, this, logs));
		}
		return this == codeTy;
	}

	@Override
	public boolean hasVar() {
		return false;
	}

	@Override
	public boolean isDynamic() {
		return false;
	}

	@Override
	public Ty staticTy() {
		return this;
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.name);
	}

	@Override
	public <C> C mapType(TypeMap<C> codeType) {
		return codeType.mapType(this.name);
	}

}
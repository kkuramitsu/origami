package blue.origami.transpiler;

import java.util.Arrays;

public class FuncTy extends Ty {
	protected final String name;
	protected final Ty[] paramTypes;
	protected final Ty returnType;

	FuncTy(String name, Ty returnType, Ty... paramTypes) {
		this.name = name;
		this.paramTypes = paramTypes;
		this.returnType = returnType;
	}

	FuncTy(Ty returnType, Ty... paramTypes) {
		this(stringfy(returnType, paramTypes), returnType, paramTypes);
	}

	public Ty getReturnType() {
		return this.returnType;
	}

	public int getParamSize() {
		return this.paramTypes.length;
	}

	public Ty[] getParamTypes() {
		return this.paramTypes;
	}

	public static String stringfy(Ty returnType, Ty... paramTypes) {
		StringBuilder sb = new StringBuilder();
		if (paramTypes.length != 1) {
			sb.append("(");
		}
		int c = 0;
		for (Ty t : paramTypes) {
			if (c > 0) {
				sb.append(",");
			}
			sb.append(t);
			c++;
		}
		if (paramTypes.length != 1) {
			sb.append(")");
		}
		sb.append("->");
		sb.append(returnType);
		return sb.toString();
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.name);
	}

	@Override
	public String strOut(TEnv env) {
		return env.format(this.name, this.name);
	}

	@Override
	public boolean isVar() {
		if (!this.returnType.isVar()) {
			for (Ty t : this.paramTypes) {
				if (t.isVar()) {
					return true;
				}
			}
			return false;
		}
		return true;
	}

	@Override
	public Ty dupTy(VarDomain dom) {
		if (this.isVar()) {
			return new FuncTy(this.name, this.returnType.dupTy(dom),
					Arrays.stream(this.paramTypes).map(x -> x.dupTy(dom)).toArray(Ty[]::new));
		}
		return this;
	}

	@Override
	public boolean acceptTy(Ty t) {
		if (t instanceof FuncTy) {
			FuncTy ft = (FuncTy) t;
			if (ft.getParamSize() != this.getParamSize()) {
				return false;
			}
			for (int i = 0; i < this.getParamSize(); i++) {
				if (!this.paramTypes[i].acceptTy(ft.paramTypes[i])) {
					return false;
				}
			}
			return this.returnType.acceptTy(ft.returnType);
		}
		return false;
	}

}
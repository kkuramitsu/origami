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
		stringfy(sb, returnType, paramTypes);
		return sb.toString();
	}

	static void stringfy(StringBuilder sb, Ty returnType, Ty... paramTypes) {
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
	}

	@Override
	public void strOut(StringBuilder sb) {
		if (this.name != null) {
			sb.append(this.name);
		} else {
			stringfy(sb, this.getReturnType(), this.getParamTypes());
		}
	}

	@Override
	public String key() {
		return this.toString();
	}

	@Override
	public String strOut(TEnv env) {
		return env.format(this.name, this.name);
	}

	@Override
	public boolean hasVar() {
		return this.returnType.hasVar() || Ty.hasVar(this.getParamTypes());
	}

	@Override
	public Ty dupTy(VarDomain dom) {
		if (this.name == null && this.hasVar()) {
			return Ty.tFunc(this.returnType.dupTy(dom),
					Arrays.stream(this.paramTypes).map(x -> x.dupTy(dom)).toArray(Ty[]::new));
		}
		return this;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty t, boolean updated) {
		if (t instanceof VarTy) {
			return (t.acceptTy(false, this, updated));
		}
		if (t instanceof FuncTy) {
			FuncTy ft = (FuncTy) t;
			if (ft.getParamSize() != this.getParamSize()) {
				return false;
			}
			for (int i = 0; i < this.getParamSize(); i++) {
				if (!this.paramTypes[i].acceptTy(false, ft.paramTypes[i], updated)) {
					return false;
				}
			}
			return this.returnType.acceptTy(false, ft.returnType, updated);
		}
		return false;
	}

	@Override
	public boolean isDynamic() {
		return Ty.isDynamic(this.getParamTypes()) || this.returnType.isDynamic();
	}

	@Override
	public Ty nomTy() {
		if (this.name == null) {
			Ty[] p = Arrays.stream(this.paramTypes).map(x -> x.nomTy()).toArray(Ty[]::new);
			Ty ret = this.returnType.nomTy();
			return Ty.tFunc(ret, p);
		}
		return this;
	}

}
package blue.origami.transpiler;

import java.util.Arrays;

public class TFuncType extends TType {
	protected final String name;
	protected final TType[] paramTypes;
	protected final TType returnType;

	TFuncType(String name, TType returnType, TType... paramTypes) {
		this.name = name;
		this.paramTypes = paramTypes;
		this.returnType = returnType;
	}

	TFuncType(TType returnType, TType... paramTypes) {
		this(stringfy(returnType, paramTypes), returnType, paramTypes);
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

	public static String stringfy(TType returnType, TType... paramTypes) {
		StringBuilder sb = new StringBuilder();
		if (paramTypes.length != 1) {
			sb.append("(");
		}
		int c = 0;
		for (TType t : paramTypes) {
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
	public String toString() {
		return this.name;
	}

	@Override
	public String strOut(TEnv env) {
		return env.format(this.name, this.name);
	}

	@Override
	public boolean isVarType() {
		if (!this.returnType.isVarType()) {
			for (TType t : this.paramTypes) {
				if (t.isVarType()) {
					return true;
				}
			}
			return false;
		}
		return true;
	}

	@Override
	public TType dup(TVarDomain dom) {
		if (this.isVarType()) {
			return new TFuncType(this.name, this.returnType.dup(dom),
					Arrays.stream(this.paramTypes).map(x -> x.dup(dom)).toArray(TType[]::new));
		}
		return this;
	}

	@Override
	public boolean acceptType(TType t) {
		if (t instanceof TFuncType) {
			TFuncType ft = (TFuncType) t;
			if (ft.getParamSize() != this.getParamSize()) {
				return false;
			}
			for (int i = 0; i < this.getParamSize(); i++) {
				if (!this.paramTypes[i].acceptType(ft.paramTypes[i])) {
					return false;
				}
			}
			return this.returnType.acceptType(ft.returnType);
		}
		return false;
	}

}
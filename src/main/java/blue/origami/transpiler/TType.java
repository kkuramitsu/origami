package blue.origami.transpiler;

import java.util.HashMap;

public abstract class TType {
	public static final TType tUntyped = new TSimpleType("?");
	public static final TType tVoid = new TSimpleType("Void");
	public static final TType tBool = new TSimpleType("Bool");
	public static final TType tInt = new TSimpleType("Int");
	public static final TType tFloat = new TSimpleType("Float");
	public static final TType tString = new TSimpleType("String");
	public static final TType tData = new TSimpleType("Data");

	private static HashMap<String, TType> typeMap = new HashMap<>();

	public static final TType tFunc(TType returnType, TType... paramTypes) {
		String key = TFuncType.stringfy(returnType, paramTypes);
		TType t = typeMap.get(key);
		if (t == null) {
			t = new TFuncType(key, returnType, paramTypes);
			typeMap.put(key, t);
		}
		return t;
	}

	//

	@Override
	public boolean equals(Object t) {
		return this == t;
	}

	public boolean isUntyped() {
		return tUntyped.equals(this);
	}

	public abstract String strOut(TEnv env);

	public final static boolean hasUntyped(TType... p) {
		for (TType t : p) {
			if (t.isUntyped()) {
				return true;
			}
		}
		return false;
	}

}

class TSimpleType extends TType {
	private String name;

	TSimpleType(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public String strOut(TEnv env) {
		return env.format(this.name, this.name);
	}

}

class TFuncType extends TType {
	protected final String name;
	protected final TType[] paramTypes;
	protected final TType returnType;

	public TFuncType(String name, TType returnType, TType... paramTypes) {
		this.name = name;
		this.paramTypes = paramTypes;
		this.returnType = returnType;
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

}
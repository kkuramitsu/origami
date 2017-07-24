package blue.origami.transpiler;

import java.util.HashMap;

import blue.origami.transpiler.code.TCode;
//import blue.origami.ocode.OCode;
import blue.origami.util.ODebug;

public abstract class TType implements TypeApi {
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

	public static final TType tVar(String name) {
		return new TVarType(name);
	}

	//

	@Override
	public boolean equals(Object t) {
		return this == t;
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

interface TypeApi {
	public default boolean isUntyped() {
		return TType.tUntyped.equals(this);
	}

	public default boolean accept(TCode code) {
		return this.equals(code.getType());
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

class TVarType extends TType {
	private String varName;
	private TType wrappedType;
	// private TType[] upperTypes = TType.emptyTypes;
	// private TType[] lowerTypes = TType.emptyTypes;

	public TVarType(String varName) {
		this.varName = varName;
		this.wrappedType = TType.tUntyped;
	}

	@Override
	public String toString() {
		return this.wrappedType.toString();
	}

	@Override
	public String strOut(TEnv env) {
		return this.wrappedType.strOut(env);
	}

	@Override
	public boolean isUntyped() {
		return this.wrappedType.isUntyped();
	}

	public void setType(TType t) {
		if (this.isUntyped() && !t.isUntyped()) {
			ODebug.trace("infer %s as %s", this.varName, t);
			this.wrappedType = t;
		}
	}

	@Override
	public boolean accept(TCode code) {
		if (this.isUntyped()) {
			TType t = code.getType(); // FIXME valueType();
			this.appendUpperBounds(t);
			return true;
		}
		return this.wrappedType.accept(code);
	}

	public void appendUpperBounds(TType t) {
		this.setType(t);
		// for (TType u : this.upperTypes) {
		// if (u.eq(t)) {
		// break;
		// }
		// }
		// append(this.upperTypes, t);
	}

	public void appendLowerBounds(TType t) {
		this.setType(t);
		// for (TType u : this.lowerTypes) {
		// if (u.eq(t)) {
		// break;
		// }
		// }
		// append(this.lowerTypes, t);
	}

}

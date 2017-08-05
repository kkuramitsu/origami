package blue.origami.transpiler;

import java.util.HashMap;

import blue.origami.transpiler.code.TBoolCode;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TDoubleCode;
import blue.origami.transpiler.code.TIntCode;
import blue.origami.transpiler.code.TStringCode;
import blue.origami.util.ODebug;

public abstract class TType implements TypeApi {

	// Core types
	public static final TType tBool = new TBoolType();
	public static final TType tInt = new TIntType();
	public static final TType tFloat = new TFloatType();
	public static final TType tString = new TStringType();
	// public static final TType tData = new TDataType();
	// Hidden types
	public static final TType tUntyped = new UntypedType("?");
	public static final TType tVoid = new TSimpleType("void");
	public static final TType tChar = new TSimpleType("char");

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

	public static final TDataType tData() {
		return new TDataType();
	}

	public static final TDataType tArray(TType innerType) {
		String key = innerType + "*";
		TType t = typeMap.get(key);
		if (t == null) {
			t = new TDataType(false, innerType);
			typeMap.put(key, t);
		}
		return (TDataType) t;
	}

	public static final TDataType tDict(TType innerType) {
		return new TDataType(true, innerType);
	}

	public static final TDataType tRecord(String... names) {
		return new TDataType(false, names);
	}

	public static final TDataType tData(String... names) {
		return new TDataType(true, names);
	}

	public static final TVarType tVar(String name) {
		return new TVarType(name);
	}

	//

	@Override
	public final boolean equals(Object t) {
		return this == t;
	}

	public abstract boolean acceptType(TType t);

	public boolean accept(TCode code) {
		return this.acceptType(code.getType());
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

	public static TType tOption(TType ty) {
		ODebug.TODO();
		return ty;
	}

	static HashMap<String, TType> hiddenMap = null;

	public static TType getHiddenType1(String tsig) {
		if (hiddenMap == null) {
			hiddenMap = new HashMap<>();
			hiddenMap.put("void", tVoid);
			hiddenMap.put("char", tChar);
			hiddenMap.put("a", TType.tVar("a"));
			hiddenMap.put("b", TType.tVar("b"));
		}
		return hiddenMap.get(tsig);
	}

	public TType realType() {
		return this;
	}

	public TType dup(TVarDomain dom) {
		return this;
	}

}

interface TypeApi {

	public default boolean isUntyped() {
		return this == TType.tUntyped;
	}

	public default boolean isVoid() {
		return this == TType.tVoid;
	}

	public default boolean isSpecific() {
		return !this.isVoid() && !this.isUntyped();
	}

	public default TCode getDefaultValue() {
		return null;
	}

	// TDataType

	public default boolean isArrayType() {
		return false;
	}

	public default TType asArrayInnerType() {
		return TType.tUntyped;
	}

	public default boolean isDictType() {
		return false;
	}

	public default TType asDictInnerType() {
		return TType.tUntyped;
	}

	// VarType

	public default boolean isVarType() {
		return false;
	}

}

class TSimpleType extends TType {
	private String name;

	TSimpleType(String name) {
		this.name = name;
	}

	@Override
	public TCode getDefaultValue() {
		return null;
	}

	@Override
	public boolean acceptType(TType t) {
		return this == t || this == t.realType();
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

class TBoolType extends TSimpleType {
	TBoolType() {
		super("Bool");
	}

	@Override
	public TCode getDefaultValue() {
		return new TBoolCode(false);
	}

}

class TIntType extends TSimpleType {
	TIntType() {
		super("Int");
	}

	@Override
	public TCode getDefaultValue() {
		return new TIntCode(0);
	}
}

class TFloatType extends TSimpleType {
	TFloatType() {
		super("Float");
	}

	@Override
	public TCode getDefaultValue() {
		return new TDoubleCode(0);
	}
}

class TStringType extends TSimpleType {
	TStringType() {
		super("String");
	}

	@Override
	public TCode getDefaultValue() {
		return new TStringCode("");
	}
}

class UntypedType extends TSimpleType {

	UntypedType(String name) {
		super(name);
	}

	@Override
	public boolean acceptType(TType t) {
		return true;
	}

}

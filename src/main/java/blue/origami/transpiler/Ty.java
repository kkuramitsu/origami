package blue.origami.transpiler;

import java.util.HashMap;

import blue.origami.transpiler.code.TBoolCode;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TDoubleCode;
import blue.origami.transpiler.code.TIntCode;
import blue.origami.transpiler.code.TStringCode;
import blue.origami.util.StringCombinator;

public abstract class Ty implements TypeApi, StringCombinator {

	// Core types
	public static final Ty tBool = new BoolTy();
	public static final Ty tInt = new IntTy();
	public static final Ty tFloat = new FloatTy();
	public static final Ty tString = new StringTy();
	// public static final TType tData = new TDataType();
	// Hidden types
	public static final Ty tUntyped = new UntypedTy("?");
	public static final Ty tVoid = new SimpleTy("()");
	public static final Ty tChar = new SimpleTy("char");
	public static final Ty tThis = null; // FIXME

	private static HashMap<String, Ty> typeMap = new HashMap<>();

	public static final Ty tFunc(Ty returnType, Ty... paramTypes) {
		String key = FuncTy.stringfy(returnType, paramTypes);
		Ty t = typeMap.get(key);
		if (t == null) {
			t = new FuncTy(key, returnType, paramTypes);
			typeMap.put(key, t);
		}
		return t;
	}

	public static final DataTy tData() {
		return new DataTy(); // growing
	}

	public static final DataTy tData(String... names) {
		return new DataTy(true, names); // dependable
	}

	public static final DataTy tImArray(Ty innerType) {
		String key = innerType + "*";
		Ty t = typeMap.get(key);
		if (t == null) {
			t = new DataTy(false, innerType).asImmutable();
			typeMap.put(key, t);
		}
		return (DataTy) t;
	}

	public static final DataTy tMArray(Ty innerType) {
		String key = "{" + innerType + "*" + "}";
		Ty t = typeMap.get(key);
		if (t == null) {
			t = new DataTy(false, innerType);
			typeMap.put(key, t);
		}
		return (DataTy) t;
	}

	public static final DataTy tImDict(Ty innerType) {
		String key = "Dict[" + innerType + "]";
		Ty t = typeMap.get(key);
		if (t == null) {
			t = new DataTy(true, innerType).asImmutable();
			typeMap.put(key, t);
		}
		return (DataTy) t;
	}

	public static final DataTy tMDict(Ty innerType) {
		String key = "$Dict[" + innerType + "]";
		Ty t = typeMap.get(key);
		if (t == null) {
			t = new DataTy(true, innerType);
			typeMap.put(key, t);
		}
		return (DataTy) t;
	}

	public static final DataTy tImRecord(String... names) {
		String key = "[" + StringCombinator.joins(names, ",") + "]";
		Ty t = typeMap.get(key);
		if (t == null) {
			t = new DataTy(false, names).asImmutable();
			typeMap.put(key, t);
		}
		return (DataTy) t;
	}

	public static final DataTy tMRecord(String... names) {
		String key = "{" + StringCombinator.joins(names, ",") + "}";
		Ty t = typeMap.get(key);
		if (t == null) {
			t = new DataTy(false, names);
			typeMap.put(key, t);
		}
		return (DataTy) t;
	}

	public static Ty tOption(Ty ty) {
		String key = ty + "?";
		Ty t = typeMap.get(key);
		if (t == null) {
			t = new OptionTy(ty);
			typeMap.put(key, t);
		}
		return ty;
	}

	public static final VarTy tVar(String name) {
		return new VarTy(name);
	}

	//

	@Override
	public final boolean equals(Object t) {
		return this == t;
	}

	public abstract boolean acceptTy(Ty t);

	public boolean accept(TCode code) {
		return this.acceptTy(code.getType());
	}

	public abstract String strOut(TEnv env);

	public final static boolean hasUntyped(Ty... p) {
		for (Ty t : p) {
			if (t.isUntyped()) {
				return true;
			}
		}
		return false;
	}

	static HashMap<String, Ty> hiddenMap = null;

	public static Ty getHidden(String tsig) {
		if (hiddenMap == null) {
			hiddenMap = new HashMap<>();
			hiddenMap.put("()", tVoid);
			hiddenMap.put("char", tChar);
			hiddenMap.put("a", Ty.tVar("a"));
			hiddenMap.put("b", Ty.tVar("b"));
		}
		return hiddenMap.get(tsig);
	}

	public Ty realTy() {
		return this;
	}

	public Ty dupTy(VarDomain dom) {
		return this;
	}

	public boolean eq(Ty ty) {
		return this == ty;
	}

	@Override
	public final String toString() {
		return StringCombinator.stringfy(this);
	}

}

interface TypeApi {

	public default boolean isUntyped() {
		return this == Ty.tUntyped;
	}

	public default boolean isVoid() {
		return this == Ty.tVoid;
	}

	public default boolean isSpecific() {
		return !this.isVoid() && !this.isUntyped();
	}

	public default TCode getDefaultValue() {
		return null;
	}

	// Option[T]

	public default boolean isOption() {
		return false;
	}

	// TDataType

	public default boolean isArray() {
		return false;
	}

	public default Ty asArrayInner() {
		return Ty.tUntyped;
	}

	public default boolean isDict() {
		return false;
	}

	public default Ty asDictInner() {
		return Ty.tUntyped;
	}

	// VarType

	public default boolean isVar() {
		return false;
	}

}

class SimpleTy extends Ty {
	private String name;

	SimpleTy(String name) {
		this.name = name;
	}

	@Override
	public TCode getDefaultValue() {
		return null;
	}

	@Override
	public boolean acceptTy(Ty t) {
		return this == t || this == t.realTy();
	}

	@Override
	public String strOut(TEnv env) {
		return env.format(this.name, this.name);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.name);
	}

}

class BoolTy extends SimpleTy {
	BoolTy() {
		super("Bool");
	}

	@Override
	public TCode getDefaultValue() {
		return new TBoolCode(false);
	}

}

class IntTy extends SimpleTy {
	IntTy() {
		super("Int");
	}

	@Override
	public TCode getDefaultValue() {
		return new TIntCode(0);
	}
}

class FloatTy extends SimpleTy {
	FloatTy() {
		super("Float");
	}

	@Override
	public TCode getDefaultValue() {
		return new TDoubleCode(0);
	}
}

class StringTy extends SimpleTy {
	StringTy() {
		super("String");
	}

	@Override
	public TCode getDefaultValue() {
		return new TStringCode("");
	}
}

class UntypedTy extends SimpleTy {

	UntypedTy(String name) {
		super(name);
	}

	@Override
	public boolean acceptTy(Ty t) {
		return true;
	}

}

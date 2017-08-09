package blue.origami.transpiler;

import java.util.HashMap;

import blue.origami.transpiler.code.BoolCode;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DoubleCode;
import blue.origami.transpiler.code.IntCode;
import blue.origami.transpiler.code.StringCode;
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

	/* DynamicType */

	public static final DataTy tData() {
		return new DataTy(); // growing
	}

	public static final DataTy tData(String... names) {
		return new DataTy(true, names); // dependable
	}

	public static final VarTy tVar(String name) {
		return new VarTy(name);
	}

	/* Data */

	public static final DataTy tImArray(Ty ty) {
		if (ty.isDynamic()) {
			return new DataTy(false, ty).asImmutable();
		}
		String key = ty + "*";
		Ty t = typeMap.get(key);
		if (t == null) {
			t = new DataTy(false, ty).asImmutable();
			typeMap.put(key, t);
		}
		return (DataTy) t;
	}

	public static final DataTy tArray(Ty ty) {
		if (ty.isDynamic()) {
			return new DataTy(false, ty);
		}
		String key = "{" + ty + "*" + "}";
		Ty t = typeMap.get(key);
		if (t == null) {
			t = new DataTy(false, ty);
			typeMap.put(key, t);
		}
		return (DataTy) t;
	}

	public static final DataTy tImDict(Ty ty) {
		if (ty.isDynamic()) {
			return new DataTy(true, ty).asImmutable();
		}
		String key = "Dict'[" + ty + "]";
		Ty t = typeMap.get(key);
		if (t == null) {
			t = new DataTy(true, ty).asImmutable();
			typeMap.put(key, t);
		}
		return (DataTy) t;
	}

	public static final DataTy tDict(Ty ty) {
		if (ty.isDynamic()) {
			return new DataTy(true, ty);
		}
		String key = "Dict[" + ty + "]";
		Ty t = typeMap.get(key);
		if (t == null) {
			t = new DataTy(true, ty);
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

	public static final DataTy tRecord(String... names) {
		String key = "{" + StringCombinator.joins(names, ",") + "}";
		Ty t = typeMap.get(key);
		if (t == null) {
			t = new DataTy(false, names);
			typeMap.put(key, t);
		}
		return (DataTy) t;
	}

	public static Ty tOption(Ty ty) {
		if (ty.isDynamic()) {
			return new OptionTy(ty).nomTy();
		}
		String key = ty + "?";
		Ty t = typeMap.get(key);
		if (t == null) {
			t = new OptionTy(ty).nomTy();
			typeMap.put(key, t);
		}
		return ty;
	}

	/* FuncType */

	public static final Ty tFunc(Ty returnType, Ty... paramTypes) {
		if (Ty.isDynamic(paramTypes) || returnType.isDynamic()) {
			return new FuncTy(null, returnType, paramTypes);
		}
		String key = FuncTy.stringfy(returnType, paramTypes);
		Ty t = typeMap.get(key);
		if (t == null) {
			t = new FuncTy(key, returnType, paramTypes);
			typeMap.put(key, t);
		}
		return t;
	}

	//

	@Override
	public final boolean equals(Object t) {
		if (t instanceof Ty) {
			return this.acceptTy(false, (Ty) t, false);
		}
		return false;
	}

	public boolean eq(Ty ty) {
		return this.acceptTy(false, ty, false);
	}

	public abstract boolean acceptTy(boolean sub, Ty t, boolean updated);

	public final boolean accept(Code code) {
		return this.acceptTy(true, code.getType(), true);
	}

	public abstract boolean isDynamic();

	public final static boolean isDynamic(Ty... p) {
		for (Ty t : p) {
			if (t.isDynamic()) {
				return true;
			}
		}
		return false;
	}

	public abstract Ty nomTy();

	// VarType

	public abstract boolean hasVar();

	public final static boolean hasVar(Ty... p) {
		for (Ty t : p) {
			if (t.hasVar()) {
				return true;
			}
		}
		return false;
	}

	public Ty dupTy(VarDomain dom) {
		return this;
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

	public abstract String key();

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

	public default Code getDefaultValue() {
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

}

class SimpleTy extends Ty {
	private String name;

	SimpleTy(String name) {
		this.name = name;
	}

	@Override
	public Code getDefaultValue() {
		return null;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty t, boolean updated) {
		if (t instanceof VarTy) {
			return (t.acceptTy(false, this, updated));
		}
		return this == t;
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
	public Ty nomTy() {
		return this;
	}

	@Override
	public String strOut(TEnv env) {
		return env.format(this.name, this.name);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.name);
	}

	@Override
	public String key() {
		return this.name;
	}

}

class BoolTy extends SimpleTy {
	BoolTy() {
		super("Bool");
	}

	@Override
	public Code getDefaultValue() {
		return new BoolCode(false);
	}

}

class IntTy extends SimpleTy {
	IntTy() {
		super("Int");
	}

	@Override
	public Code getDefaultValue() {
		return new IntCode(0);
	}
}

class FloatTy extends SimpleTy {
	FloatTy() {
		super("Float");
	}

	@Override
	public Code getDefaultValue() {
		return new DoubleCode(0);
	}
}

class StringTy extends SimpleTy {
	StringTy() {
		super("String");
	}

	@Override
	public Code getDefaultValue() {
		return new StringCode("");
	}
}

class UntypedTy extends SimpleTy {

	UntypedTy(String name) {
		super(name);
	}

	@Override
	public boolean acceptTy(boolean sub, Ty t, boolean updated) {
		return true;
	}

}

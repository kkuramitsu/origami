package blue.origami.transpiler;

import java.util.HashMap;
import java.util.function.Supplier;

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

	public static final Ty tUntyped0 = new UntypedTy("?");
	public static final Ty tVoid = new SimpleTy("()");
	public static final Ty tChar = new SimpleTy("char");
	public static final Ty tThis = new SimpleTy("_");
	public static final Ty tAuto = new SimpleTy("auto");

	private static HashMap<String, Ty> typeMap = new HashMap<>();

	/* DynamicType */

	public static final DataTy tData() {
		return new DataTy();
	}

	public static final DataTy tData(String... names) {
		return new DataTy(names);
	}

	public static final VarTy tUntyped() {
		return new VarTy(null);
	}

	public static final VarTy tVar(String name) {
		return new VarTy(name);
	}

	/* Data */

	static Ty reg(String key, Supplier<Ty> sup) {
		Ty t = typeMap.get(key);
		if (t == null) {
			t = sup.get();
			typeMap.put(key, t);
		}
		return t;
	}

	public static final ArrayTy tImArray(Ty ty) {
		if (ty.isDynamic()) {
			return new ArrayTy(ty).asImmutable();
		}
		return (ArrayTy) reg(ty + "*", () -> new ArrayTy(ty).asImmutable());
	}

	public static final ArrayTy tArray(Ty ty) {
		if (ty.isDynamic()) {
			return new ArrayTy(ty);
		}
		return (ArrayTy) reg(ty + "[]", () -> new ArrayTy(ty));
	}

	public static final DictTy tImDict(Ty ty) {
		if (ty.isDynamic()) {
			return new DictTy(ty).asImmutable();
		}
		return (DictTy) reg("Dict'[" + ty + "]", () -> new DictTy(ty).asImmutable());
	}

	public static final DictTy tDict(Ty ty) {
		if (ty.isDynamic()) {
			return new DictTy(ty);
		}
		return (DictTy) reg("Dict[" + ty + "]", () -> new DictTy(ty));
	}

	public static final DataTy tImRecord(String... names) {
		return (DataTy) reg("[" + StringCombinator.joins(names, ",") + "]", () -> new DataTy(names).asImmutable());
	}

	public static final DataTy tRecord(String... names) {
		return (DataTy) reg("{" + StringCombinator.joins(names, ",") + "}", () -> new DataTy(names));
	}

	public static OptionTy tOption(Ty ty) {
		if (ty instanceof OptionTy) {
			return (OptionTy) ty;
		}
		if (ty.isDynamic()) {
			return new OptionTy(ty);
		}
		return (OptionTy) reg(ty + "?", () -> new OptionTy(ty));
	}

	/* FuncType */

	public static final FuncTy tFunc(Ty returnType, Ty... paramTypes) {
		if (Ty.isDynamic(paramTypes) || returnType.isDynamic()) {
			return new FuncTy(null, returnType, paramTypes);
		}
		String key = FuncTy.stringfy(returnType, paramTypes);
		return (FuncTy) reg(key, () -> new FuncTy(key, returnType, paramTypes));
	}

	//

	@Override
	public Ty type() {
		return this;
	}

	public Ty getInnerTy() {
		return null;
	}

	public final static boolean isUntyped(Ty t) {
		return t == null || t.isUntyped();
	}

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

	public abstract boolean acceptTy(boolean sub, Ty codeTy, boolean updated);

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

	// public final static boolean hasUntyped(Ty... p) {
	// for (Ty t : p) {
	// if (t.isUntyped()) {
	// return true;
	// }
	// }
	// return false;
	// }

	static HashMap<String, Ty> hiddenMap = null;

	public static Ty getHidden1(String tsig) {
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

	public static Ty selfTy(Ty ty, DataTy dt) {
		return ty == Ty.tThis ? dt : ty;
	}

}

interface TypeApi {

	public Ty type();

	public default boolean is(Ty ty) {
		return type() == ty;
	}

	public default boolean isVoid() {
		return type() == Ty.tVoid;
	}

	public default boolean isOption() {
		return type() instanceof OptionTy;
	}

	public default boolean isFunc() {
		return type() instanceof FuncTy;
	}

	public default boolean isData() {
		return type() instanceof DataTy;
	}

	public default boolean isArray() {
		return type() instanceof ArrayTy;
	}

	public default boolean isDict() {
		return type() instanceof DictTy;
	}

	// VarType a

	public default boolean isVarRef() {
		return false;
	}

	public default boolean isUntyped() {
		return this == Ty.tUntyped0;
	}

	public default boolean isSpecific() {
		return !this.isVoid() && !this.isUntyped();
	}

	public default Code getDefaultValue() {
		return null;
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
	public boolean acceptTy(boolean sub, Ty codeTy, boolean updated) {
		if (codeTy instanceof VarTy) {
			return (codeTy.acceptTy(false, this, updated));
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
	public boolean acceptTy(boolean sub, Ty codeTy, boolean updated) {
		return true;
	}

}

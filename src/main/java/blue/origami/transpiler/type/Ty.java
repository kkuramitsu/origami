package blue.origami.transpiler.type;

import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Supplier;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.code.BoolCode;
import blue.origami.transpiler.code.CastCode;
import blue.origami.transpiler.code.CastCode.TConvTemplate;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DoubleCode;
import blue.origami.transpiler.code.IntCode;
import blue.origami.transpiler.code.MultiCode;
import blue.origami.transpiler.code.StringCode;
import blue.origami.util.StringCombinator;

public abstract class Ty implements TypeApi, StringCombinator {

	// Core types
	public static final Ty tVoid = new VoidTy();
	public static final Ty tBool = new BoolTy();
	public static final Ty tInt = new IntTy();
	public static final Ty tFloat = new FloatTy();
	public static final Ty tString = new StringTy();

	// Hidden Type
	public static final Ty tAnyRef = new AnyTy();
	public static final Ty tByte = new SimpleTy("Byte");
	public static final Ty tInt64 = new SimpleTy("Int64");
	public static final Ty tFloat32 = new SimpleTy("Float32");
	public static final Ty tChar = new SimpleTy("Char");

	public static final Ty tThis = new SimpleTy("_");
	public static final Ty tAuto = new SimpleTy("auto");

	private static HashMap<String, Ty> typeMap = new HashMap<>();

	static {
		typeMap.put("Option", new OptionTy("Option", tVoid));
		typeMap.put("List", new ListTy("List", tVoid));
		typeMap.put("List'", new ListTy("List'", tVoid));
		typeMap.put("Stream", new MonadTy("Stream", tVoid));
		typeMap.put("Dict", new DictTy("Dict", tVoid));
		typeMap.put("Dict'", new DictTy("Dict'", tVoid));
	}

	/* DynamicType */

	public static final DataTy tData() {
		return new DataTy();
	}

	// public static final DataTy tData(String... names) {
	// return new DataTy(names);
	// }

	public static final VarTy tUntyped() {
		return new VarTy(null, null);
	}

	public static final VarTy tUntyped(Tree<?> s) {
		return new VarTy(null, s);
	}

	public static final VarTy tVar(String name) {
		return new VarTy(name, null);
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

	public static final boolean isMonad(String name) {
		return typeMap.get(name) instanceof MonadTy;
	}

	public static final Ty tMonad(String name, Ty ty) {
		MonadTy monad = (MonadTy) typeMap.get(name);
		assert (monad != null) : "undefined " + name;
		if (ty.isDynamic()) {
			return monad.newType(name, ty);
		}
		String key = name + "[" + ty + "]";
		return reg(key, () -> monad.newType(name, ty));
	}

	public static final ListTy tImList(Ty ty) {
		return (ListTy) tMonad("List", ty);
	}

	public static final ListTy tList(Ty ty) {
		return (ListTy) tMonad("List'", ty);
	}

	public static final DataTy tRecord(String... names) {
		return (DataTy) reg("[" + StringCombinator.joins(names, ",") + "]", () -> new DataTy(names).asImmutable());
	}

	public static final DataTy tData(String... names) {
		return (DataTy) reg("{" + StringCombinator.joins(names, ",") + "}", () -> new DataTy(names));
	}

	public static OptionTy tOption(Ty ty) {
		return (OptionTy) tMonad("Option", ty);
	}

	/* FuncType */

	public static final FuncTy tFunc(Ty returnType, Ty... paramTypes) {
		if (Ty.isDynamic(paramTypes) || returnType.isDynamic()) {
			return new FuncTy(null, returnType, paramTypes);
		}
		String key = FuncTy.stringfy(returnType, paramTypes);
		return (FuncTy) reg(key, () -> new FuncTy(key, returnType, paramTypes));
	}

	public static Ty tTag(Ty inner, String... names) {
		Arrays.sort(names);
		return new TagTy(inner, names);
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
		return t == null;
	}

	@Override
	public final boolean equals(Object t) {
		if (t instanceof Ty) {
			return this.acceptTy(false, (Ty) t, VarLogger.Nop);
		}
		return false;
	}

	public boolean eq(Ty ty) {
		return this.acceptTy(false, ty, VarLogger.Nop);
	}

	public abstract boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs);

	public final boolean accept(Code code) {
		Ty codeTy = code.getType();
		return this == codeTy || this.acceptTy(true, codeTy, VarLogger.Update);
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

	public abstract Ty staticTy();

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

	public Ty dupVarType(VarDomain dom) {
		return this;
	}

	public boolean isMutable() {
		return false;
	}

	public Ty toImmutable() {
		return this;
	}

	// public Ty returnTy(TEnv env) {
	// return this;
	// }

	// public abstract String key();

	@Override
	public final String toString() {
		return StringCombinator.stringfy(this);
	}

	public static Ty selfTy(Ty ty, DataTy dt) {
		return ty == Ty.tThis ? dt : ty;
	}

	public abstract <C> C mapType(TypeMap<C> codeType);

}

interface TypeApi {

	public Ty type();

	public default boolean is(Ty ty) {
		return type() == ty;
	}

	public default boolean isVoid() {
		return type() == Ty.tVoid;
	}

	public default boolean isAnyRef() {
		return type() instanceof AnyTy;
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

	public default boolean isList() {
		return type() instanceof ListTy;
	}

	public default boolean isDict() {
		return type() instanceof DictTy;
	}

	// VarType a

	public default boolean isVarRef() {
		return false;
	}

	public default boolean isSpecific() {
		return !this.isVoid();
	}

	public default Code getDefaultValue() {
		return null;
	}

	public default int costMapTo(TEnv env, Ty toTy) {
		return CastCode.STUPID;
	}

	public default Template findMapTo(TEnv env, Ty toTy) {
		return null;
	}

	public default int costMapFrom(TEnv env, Ty fromTy) {
		return CastCode.STUPID;
	}

	public default Template findMapFrom(TEnv env, Ty fromTy) {
		return null;
	}

}

class VoidTy extends SimpleTy {
	VoidTy() {
		super("()");
	}

	@Override
	public Code getDefaultValue() {
		return new MultiCode();
	}

	@Override
	public int costMapFrom(TEnv env, Ty fromTy) {
		return CastCode.SAME;
	}

	@Override
	public Template findMapFrom(TEnv env, Ty fromTy) {
		String format = env.getSymbol("(Void)", "(void)%s");
		return new TConvTemplate("", fromTy, Ty.tVoid, CastCode.SAME, format);
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
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		return true;
	}

}

package blue.origami.transpiler.type;

import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Supplier;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TArrays;
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
import blue.origami.util.OStrings;

public abstract class Ty implements TypeApi, OStrings {

	// Core types
	public static final Ty tVoid = new VoidTy();
	public static final Ty tBool = new BoolTy();
	public static final Ty tInt = new IntTy();
	public static final Ty tFloat = new FloatTy();
	public static final Ty tString = new StringTy();

	// Hidden Type
	public static final Ty tAny = new AnyTy();
	public static final Ty tByte = new SimpleTy("Byte");
	public static final Ty tInt64 = new SimpleTy("Int64");
	public static final Ty tFloat32 = new SimpleTy("Float32");
	public static final Ty tChar = new SimpleTy("Char");

	public static final Ty tNULL = new SimpleTy("?");
	public static final Ty tThis = new SimpleTy("_");
	public static final Ty tAuto = new SimpleTy("auto");

	private static HashMap<String, Ty> typeMemoMap = new HashMap<>();

	static {
		typeMemoMap.put("Option", new OptionTy("Option", tVoid));
		typeMemoMap.put("List", new ListTy("List", tVoid));
		typeMemoMap.put("List'", new ListTy("List'", tVoid));
		typeMemoMap.put("Stream", new MonadTy("Stream", tVoid));
		typeMemoMap.put("Stream'", new MonadTy("Stream'", tVoid));
		typeMemoMap.put("Dict", new DictTy("Dict", tVoid));
		typeMemoMap.put("Dict'", new DictTy("Dict'", tVoid));
	}

	/* DynamicType */

	public static final DataTy tData() {
		return new DataTy();
	}

	// public static final DataTy tData(String... names) {
	// return new DataTy(names);
	// }

	public static final VarTy tUntyped() {
		return new VarTy(null, -1);
	}

	public static final VarTy tUntyped(Tree<?> s) {
		return new VarTy(null, -1);
	}

	// public static final VarTy tVar(String name) {
	// return new VarTy(name, null);
	// }

	/* Data */

	public abstract boolean isNonMemo();

	static Ty reg(String key, Supplier<Ty> sup) {
		Ty t = typeMemoMap.get(key);
		if (t == null) {
			t = sup.get();
			typeMemoMap.put(key, t);
		}
		return t;
	}

	public static final boolean isDefinedMonad(String name) {
		return typeMemoMap.get(name) instanceof MonadTy;
	}

	public static final Ty tMonad(String name, Ty ty) {
		MonadTy monad = (MonadTy) typeMemoMap.get(name);
		assert (monad != null) : "undefined " + name;
		if (!ty.isNonMemo()) {
			return monad.newType(name, ty);
		}
		String key = name + "[" + ty + "]";
		return reg(key, () -> monad.newType(name, ty));
	}

	public static final ListTy tList(Ty ty) {
		return (ListTy) tMonad("List", ty);
	}

	public static final ListTy tArray(Ty ty) {
		return (ListTy) tMonad("List'", ty);
	}

	public static final DataTy tRecord(String... names) {
		Arrays.sort(names);
		return (DataTy) reg("[" + OStrings.joins(names, ",") + "]", () -> new DataTy(false, names));
	}

	public static final DataTy tData(String... names) {
		Arrays.sort(names);
		return (DataTy) reg("{" + OStrings.joins(names, ",") + "}", () -> new DataTy(true, names));
	}

	public static OptionTy tOption(Ty ty) {
		return (OptionTy) tMonad("Option", ty);
	}

	/* FuncType */

	public static final FuncTy tFunc(Ty returnType, Ty... paramTypes) {
		if (TArrays.testSomeTrue(t -> t.isNonMemo(), paramTypes) || returnType.isNonMemo()) {
			return new FuncTy(null, returnType, paramTypes);
		}
		String key = FuncTy.stringfy(returnType, paramTypes);
		return (FuncTy) reg(key, () -> new FuncTy(key, returnType, paramTypes));
	}

	public static final TupleTy tTuple(Ty... paramTypes) {
		if (TArrays.testSomeTrue(t -> t.isNonMemo(), paramTypes)) {
			return new TupleTy(null, paramTypes);
		}
		String key = TupleTy.stringfy(paramTypes);
		return (TupleTy) reg(key, () -> new TupleTy(key, paramTypes));
	}

	public static Ty tTag(Ty inner, String... names) {
		Arrays.sort(names);
		return new TagTy(inner, names);
	}

	//

	public final static boolean isUntyped(Ty t) {
		return t == null;
	}

	@Override
	public Ty real() {
		return this;
	}

	public Ty getInnerTy() {
		return null;
	}

	@Override
	public final boolean equals(Object t) {
		if (t instanceof Ty) {
			return this.toString().equals(t.toString());
			// return this.acceptTy(false, (Ty) t, VarLogger.Nop);
		}
		return false;
	}

	public boolean eq(Ty ty) {
		return this.acceptTy(false, ty, VarLogger.Nop);
	}

	public final boolean accept(Code code) {
		Ty codeTy = code.getType();
		return this == codeTy || this.acceptTy(true, codeTy, VarLogger.Update);
	}

	public abstract boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs);

	protected boolean acceptVarTy(boolean sub, Ty codeTy, VarLogger logs) {
		if (codeTy.isVar()) {
			return (codeTy.acceptTy(false, this, logs));
		}
		return false;
	}

	// public abstract Ty staticTy();

	public abstract boolean hasVar();

	public Ty dupVar(VarDomain dom) {
		return this;
	}

	public boolean isAmbigous() {
		return this.hasVar() || this.isUnion();
	}

	public boolean isMutable() {
		return false;
	}

	public Ty toImmutable() {
		return this;
	}

	public boolean hasMutation() {
		return this.isMutable();
	}

	public void hasMutation(boolean b) {
	}

	public Ty finalTy() {
		return this;
	}

	@Override
	public final String toString() {
		return OStrings.stringfy(this);
	}

	// public static Ty selfTy(Ty ty, DataTy dt) {
	// return ty == Ty.tThis ? dt : ty;
	// }

	public abstract <C> C mapType(TypeMap<C> codeType);

}

interface TypeApi {

	public Ty real();

	public default boolean is(Ty ty) {
		return real() == ty;
	}

	public default boolean isVoid() {
		return real() == Ty.tVoid;
	}

	public default boolean isVar() {
		return real() instanceof VarTy;
	}

	public default boolean isAny() {
		return real() instanceof AnyTy;
	}

	public default boolean isNULL() {
		return real() == Ty.tNULL;
	}

	public default boolean isOption() {
		return real() instanceof OptionTy;
	}

	public default boolean isFunc() {
		return real() instanceof FuncTy;
	}

	public default boolean isTuple() {
		return real() instanceof TupleTy;
	}

	public default boolean isUnion() {
		return real() instanceof UnionTy;
	}

	public default boolean isData() {
		return real() instanceof DataTy;
	}

	public default boolean isList() {
		return real() instanceof ListTy;
	}

	public default boolean isDict() {
		return real() instanceof DictTy;
	}

	public default boolean isMonad(String name) {
		Ty ty = real();
		if (ty instanceof MonadTy) {
			return ((MonadTy) ty).equalsName(name);
		}
		return false;
	}

	public default boolean isSpecific() {
		return !this.isVoid() && !this.isUnion();
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

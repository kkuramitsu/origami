package blue.origami.transpiler.type;

import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import blue.origami.common.OArrays;
import blue.origami.common.OStrings;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;

// Base = Bool, Int, Float, String
//        a,b,c
// T[T] : Option[T], List[T], Map[T]
// T*T  : Tuple
// T->T : Func
// {key: a, value: b}
// Mut  : T$
// Var  : %s a

public abstract class Ty implements TypeApi, OStrings {

	@Override
	public final String toString() {
		return OStrings.stringfy(this);
	}

	// Key

	// keyOfArrow
	public abstract String keyOfArrows();

	// keyOfMemoMap
	public String keyOfMemo() {
		return this.toString();
	}

	// Memo
	private int memoId = 0;
	static private HashMap<String, Ty> memoMap = new HashMap<>();

	public static Ty t(String id) {
		return memoMap.get(id);
	}

	protected boolean isMemoed() {
		return this.memoId > 0;
	}

	static char NonMemoChar = '%';
	static String NonMemoStr = "%";

	static Ty memo(Ty ty) {
		String id = ty.keyOfMemo();
		if (id.indexOf(NonMemoChar) >= 0) {
			return ty;
		}
		Ty ty2 = memoMap.get(id);
		if (ty2 == null) {
			memoMap.put(id, ty);
			ty.memoId = memoMap.size();
			ty2 = ty;
		}
		return ty2;
	}

	static Ty m(Ty ty) {
		return (ty.isMemoed()) ? ty : memo(ty);
	}

	static Ty[] m(Ty[] ty) {
		return Arrays.stream(ty).map(t -> m(t)).toArray(Ty[]::new);
	}

	/* structure */

	public Ty getParamType() {
		return null;
	}

	public static Predicate<Ty> IsVar = (t) -> t instanceof VarTy;
	public static Predicate<Ty> IsGeneric = (t) -> t instanceof VarParamTy;
	public static Predicate<Ty> IsVarParam = (t) -> (t instanceof VarParamTy || t instanceof VarTy);

	public abstract boolean hasSome(Predicate<Ty> f);

	public abstract Ty map(Function<Ty, Ty> f);

	public static Ty[] map(Ty[] ts, Function<Ty, Ty> f) {
		Ty[] p = new Ty[ts.length];
		for (int i = 0; i < p.length; i++) {
			p[i] = f.apply(ts[i]);
		}
		return p;
	}

	/* match */

	public final boolean match(TypeMatchContext tmx, boolean sub, Ty right) {
		Ty left = this.devar();
		right = right.devar();
		if (left.isVar()) {
			right = right.inferType(tmx);
			return (((VarTy) left).matchVar(tmx, right));
		}
		if (right.isVar()) {
			left = left.inferType(tmx);
			return (((VarTy) right).matchVar(tmx, left));
		}
		// System.out.println("BASE: " + left + ", " + right + ", " +
		// left.matchBase(sub, right));
		if (left.matchBase(sub, right)) {
			int psize = left.paramSize();
			for (int i = 0; i < psize; i++) {
				if (!left.param(i).match(tmx, false, right.param(i))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public Ty inferType(TypeMatchContext tmx) {
		return this;
	}

	boolean matchBase(boolean sub, Ty right) {
		assert !(right instanceof VarTy);
		return right.eq(this) || (sub && right.hasSuperType(this));
	}

	public boolean hasSuperType(Ty left) {
		return left == this || left.eq(this);
	}

	public int paramSize() {
		return 0;
	}

	public Ty[] params() {
		return OArrays.emptyTypes;
	}

	public Ty param(int n) {
		return null;
	}

	// public abstract boolean hasSuperType(Ty left);

	@Override
	public final boolean equals(Object t) {
		if (t instanceof Ty) {
			return this.eq((Ty) t);
		}
		return false;
	}

	public abstract boolean eq(Ty ty);

	public final boolean match(Code code) {
		return this.match(code.getType());
	}

	public final boolean match(Ty right) {
		return this == right || this.match(TypeMatchContext.Update, true, right);
	}

	/* Mutable */

	public boolean isMutable() {
		return false;
	}

	public Ty toMutable() {
		if (!this.isMutable()) {
			return Ty.m(new MutableTy(this));
		}
		return this;
	}

	public Ty toImmutable() {
		return this;
	}

	/* conversion */

	@Override
	public Ty devar() {
		return this;
	}

	public Ty newGeneric(Ty m) {
		return this;
	}

	public Ty memoed() {
		return this;
	}

	public Ty dupVar(VarDomain dom) {
		return this;
	}

	public abstract <C> C mapType(TypeMapper<C> codeType);

	/* static */

	public final static String Mut = "$";

	// Core types
	public static final Ty tVoid = m(new VoidTy());
	public static final Ty tBool = m(new BoolTy());
	public static final Ty tInt = m(new IntTy());
	public static final Ty tFloat = m(new FloatTy());
	public static final Ty tString = m(new StringTy());
	//
	public static final Ty tOption = m(new OptionTy());
	public static final Ty tList = m(new BaseTy("List", 1));
	public static final Ty tDict = m(new BaseTy("Dict", 1));
	public static final Ty tStream = m(new BaseTy("Stream", 1));

	// VarParam
	public static final Ty[] tVarParam = new Ty[26];
	static {
		for (int i = 0; i < tVarParam.length; i++) {
			tVarParam[i] = m(new VarParamTy(String.valueOf((char) ('a' + i))));
		}
	}

	public static final Ty tVarParam(String varname) {
		char ch = varname.charAt(0);
		assert (Character.isLowerCase(ch));
		return tVarParam[(ch - 'a')];
	}

	// Hidden Type
	public static final Ty tAnyRef = m(new AnyRefTy("AnyRef"));
	public static final Ty tByte = m(new BaseTy("Byte"));
	public static final Ty tInt64 = m(new BaseTy("Int64"));
	public static final Ty tFloat32 = m(new BaseTy("Float32"));
	public static final Ty tChar = m(new BaseTy("Char"));

	// public static final Ty tNULL = m(new SimpleTy("?"));
	public static final Ty tThis = m(new BaseTy("_"));
	public static final Ty tAuto = m(new BaseTy("auto"));

	/* DynamicType */

	public static final DataTy tData() {
		return new DataTy();
	}

	// public static final VarTy tVar() {
	// return new VarTy("");
	// }

	public static final VarTy tVar(AST s) {
		return new VarTy(s);
	}

	/* Data */

	// public static final Ty t(String name) {
	// return Memo.t(name);
	// }
	//
	// public static final Ty tGeneric(String base, Ty param) {
	// return m(MemoType.t(base).newGeneric(m(param)));
	// }

	public static final Ty tGeneric(Ty base, Ty param) {
		return m(m(base).newGeneric(m(param)));
	}

	public static Ty tOption(Ty ty) {
		return tGeneric(tOption, ty);
	}

	public static final Ty tList(Ty ty) {
		return tGeneric(tList, ty);
	}

	// public static final Ty tArray(Ty ty) {
	// return tGeneric(tMList, ty);
	// }

	// public static final DataTy tRecord(String... names) {
	// Arrays.sort(names);
	// return (DataTy) m(new DataTy(names));
	// }

	public static final DataTy tData(String... names) {
		Arrays.sort(names);
		return (DataTy) m(new DataTy(names));
	}

	/* FuncType */

	public static final FuncTy tFunc(Ty returnType, Ty... paramTypes) {
		return (FuncTy) m(new FuncTy(m(returnType), m(paramTypes)));
	}

	public static final Ty tTuple(Ty... ts) {
		Ty ty = m(new TupleTy(m(ts)));
		// System.out.println("********* " + ty + " memoed=" + ty.isMemoed());
		return ty;
	}

	public static Ty tTag(Ty base, String... names) {
		if (base instanceof TagTy) {
			TagTy tag = (TagTy) base;
			base = tag.getParamType();
			names = TagTy.joins(names, tag.tags);
		}
		if (names.length == 0) {
			return m(base);
		}
		Arrays.sort(names);
		return m(new TagTy(m(base), names));
	}

	public static final Ty tCond(Ty base, boolean pred, Ty cond) {
		return m(base);
		// return m(new CondTy(m(base), pred, m(cond)));
	}

	//

	public final static boolean isUntyped(Ty t) {
		return t == null;
	}

	// public abstract Ty staticTy();

	public boolean isAmbigous() {
		return this.hasSome(Ty.IsVar) /* || this.isUnion() */;
	}

	public void typeKey(StringBuilder sb) {
		this.strOut(sb);
	}

	public static String mapKey2(Ty fromTy, Ty toTy) {
		StringBuilder sb = new StringBuilder();
		fromTy.typeKey(sb);
		sb.append("->");
		toTy.typeKey(sb);
		return sb.toString();
	}

}

interface TypeApi {

	public Ty devar();

	public default boolean is(Ty ty) {
		return devar() == ty;
	}

	public default boolean isVoid() {
		return devar() == Ty.tVoid;
	}

	public default boolean isMut() {
		return devar() instanceof MutableTy;
	}

	public default boolean isVar() {
		return devar() instanceof VarTy;
	}

	public default boolean isAny() {
		return devar() instanceof VarParamTy;
	}
	//
	// public default boolean isOption() {
	// return real() instanceof OptionTy;
	// }

	public default boolean isFunc() {
		return devar() instanceof FuncTy;
	}

	public default boolean isTuple() {
		return devar() instanceof TupleTy;
	}

	// public default boolean isUnion() {
	// return real() instanceof UnionTy;
	// }

	public default boolean isData() {
		return devar() instanceof DataTy;
	}

	public default boolean isGeneric() {
		return devar() instanceof GenericTy;
	}

	public default boolean isGeneric(Ty baseTy) {
		Ty ty = devar();
		if (ty instanceof GenericTy) {
			return ((GenericTy) ty).getBaseType().devar() == baseTy;
		}
		return false;
	}

	public default boolean isSpecific() {
		return !this.isVoid(); /* && !this.isUnion(); */
	}

	public default Ty resolveFieldType(Env env, AST s, String name) {
		throw new ErrorCode(s, TFmt.undefined_name__YY1_in_YY2, name, this);
	}

	public default int costMapFromToThis(Env env, Ty fromTy, Ty toTy) {
		return CodeMap.STUPID;
	}

	public default CodeMap findMapFromToThis(Env env, Ty fromTy, Ty toTy) {
		return null;
	}

	public default int costMapThisTo(Env env, Ty fromTy, Ty toTy) {
		return CodeMap.STUPID;
	}

	public default CodeMap findMapThisTo(Env env, Ty fromTy, Ty toTy) {
		return null;
	}

}

class VoidTy extends BaseTy {
	VoidTy() {
		super("()");
	}

	@Override
	public int costMapFromToThis(Env env, Ty fromTy, Ty toTy) {
		return CodeMap.SAME;
	}

	@Override
	public CodeMap findMapFromToThis(Env env, Ty fromTy, Ty toTy) {
		return new CodeMap(CodeMap.SAME | CodeMap.LazyFormat, "(void)", "voidcast", fromTy, Ty.tVoid);
	}

}

class BoolTy extends BaseTy {
	BoolTy() {
		super("Bool");
	}

}

class IntTy extends BaseTy {
	IntTy() {
		super("Int");
	}
}

class FloatTy extends BaseTy {
	FloatTy() {
		super("Float");
	}
}

class StringTy extends BaseTy {
	StringTy() {
		super("String");
	}

}

class UntypedTy extends BaseTy {

	UntypedTy(String name) {
		super(name);
	}

}

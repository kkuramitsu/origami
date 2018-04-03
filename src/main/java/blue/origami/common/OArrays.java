package blue.origami.common;

import java.lang.reflect.Array;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.type.Ty;
import origami.nez2.ParseTree;
import origami.nez2.Token;

public class OArrays {
	// avoid duplicated empty array;
	public static final Ty[] emptyTypes = new Ty[0];
	public static final Code[] emptyCodes = new Code[0];
	public static final String[] emptyNames = new String[0];
	public static final ParseTree[] emptyASTs = new ParseTree[0];
	public static final Token[] emptyTokens = new Token[0];

	public static <T> T[] join(IntFunction<T[]> gen, T a, @SuppressWarnings("unchecked") T... as) {
		T[] p = gen.apply(as.length + 1);
		p[0] = a;
		System.arraycopy(as, 0, p, 1, as.length);
		return p;

	}

	public static Ty[] join(Ty first, Ty... params) {
		Ty[] p = new Ty[params.length + 1];
		p[0] = first;
		System.arraycopy(params, 0, p, 1, params.length);
		return p;
	}

	public static Code[] join(Code first, Code... params) {
		Code[] p = new Code[params.length + 1];
		p[0] = first;
		System.arraycopy(params, 0, p, 1, params.length);
		return p;
	}

	public static Ty[] ltrim(Ty... params) {
		Ty[] p = new Ty[params.length - 1];
		System.arraycopy(params, 1, p, 0, params.length - 1);
		return p;
	}

	public static Code[] ltrim(Code... params) {
		Code[] p = new Code[params.length - 1];
		System.arraycopy(params, 1, p, 0, params.length - 1);
		return p;
	}

	public static <T> T[] ltrim2(T[] values) {
		@SuppressWarnings("unchecked")
		T[] v = (T[]) Array.newInstance(values.getClass().getComponentType(), values.length - 1);
		System.arraycopy(values, 1, v, 0, v.length);
		return v;
	}

	public static <T> T[] rtrim2(T[] values) {
		@SuppressWarnings("unchecked")
		T[] v = (T[]) Array.newInstance(values.getClass().getComponentType(), values.length - 1);
		System.arraycopy(values, 0, v, 0, v.length);
		return v;
	}

	public static String[] names(int length) {
		String[] names = new String[length];
		for (int c = 0; c < length; c++) {
			names[c] = String.valueOf((char) ('a' + c));
		}
		return names;
	}

	public static <T> boolean testSome(T[] list, Predicate<T> f) {
		for (T e : list) {
			if (f.test(e)) {
				return true;
			}
		}
		return false;
	}

	public static <T> boolean testAll(T[] list, Predicate<T> f) {
		for (T e : list) {
			if (!f.test(e)) {
				return false;
			}
		}
		return true;
	}

}

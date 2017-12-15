/***********************************************************************
 * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***********************************************************************/

package blue.origami.asm;

import java.util.Objects;

import blue.origami.chibi.Func.FuncObjObj;
import blue.origami.chibi.Func.FuncObjVoid;
import blue.origami.chibi.List$;
import blue.origami.chibi.List$Float;
import blue.origami.chibi.List$Int;
import blue.origami.chibi.Range$Int;

public class APIs {
	public final static Object box(boolean a) {
		return a;
	}

	public final static boolean unboxZ(Object a) {
		return (Boolean) a;
	}

	public final static Object box(char a) {
		return a;
	}

	public final static char unboxC(Object a) {
		return ((Character) a).charValue();
	}

	public final static Object box(byte a) {
		return a;
	}

	public final static byte unboxB(Object a) {
		return (byte) ((Number) a).intValue();
	}

	public final static Object box(short a) {
		return a;
	}

	public final static short unboxS(Object a) {
		return (short) ((Number) a).intValue();
	}

	public final static Object box(int a) {
		return a;
	}

	public final static int unboxI(Object a) {
		return ((Number) a).intValue();
	}

	public final static Object box(double a) {
		return a;
	}

	public final static double unboxD(Object a) {
		return ((Number) a).doubleValue();
	}

	public final static Object box(long a) {
		return a;
	}

	public final static long unboxL(Object a) {
		return ((Number) a).longValue();
	}

	/* Boolean */

	public final static boolean not(boolean a) {
		return !a;
	}

	public final static boolean eq(boolean a, boolean b) {
		return a == b;
	}

	public final static boolean ne(boolean a, boolean b) {
		return a != b;
	}

	// -- assert --

	private static int testCount = 0;
	private static int passCount = 0;

	public final static void testAssert(boolean a) {
		testCount++;
		assert (a);
		passCount++;
	}

	public final static int getTestCount() {
		return testCount;
	}

	public final static int getPassCount() {
		return passCount;
	}

	public final static void resetCount() {
		testCount = 0;
		passCount = 0;
	}

	public final static String toString(boolean a) {
		return String.valueOf(a);
	}

	/* int */

	public final static boolean eq(int a, int b) {
		return a == b;
	}

	public final static boolean ne(int a, int b) {
		return a != b;
	}

	public final static boolean lt(int a, int b) {
		return a < b;
	}

	public final static boolean gt(int a, int b) {
		return a > b;
	}

	public final static boolean lte(int a, int b) {
		return a <= b;
	}

	public final static boolean gte(int a, int b) {
		return a >= b;
	}

	public final static int cmpl(int a) {
		return ~a;
	}

	public final static int pow(int a, int b) {
		return (int) Math.pow(a, b);
	}

	public final static String toString(int a) {
		return String.valueOf(a);
	}

	/* Double */

	public final static boolean eq(double a, double b) {
		return a == b;
	}

	public final static boolean ne(double a, double b) {
		return a != b;
	}

	public final static boolean lt(double a, double b) {
		return a < b;
	}

	public final static boolean gt(double a, double b) {
		return a > b;
	}

	public final static boolean lte(double a, double b) {
		return a <= b;
	}

	public final static boolean gte(double a, double b) {
		return a >= b;
	}

	/* Conversion */

	public final static String toString(double a) {
		return String.valueOf(a);
	}

	/* String */

	public static final int size(String x) {
		return x.length();
	}

	public static final String get(String x, int n) {
		return String.valueOf(x.charAt(n));
	}

	public static final String add(String x, String y) {
		return x + y;
	}

	public static final boolean eq(String x, String y) {
		return Objects.equals(x, y);
	}

	public static final boolean ne(String x, String y) {
		return !Objects.equals(x, y);
	}

	public final static boolean lt(String a, String b) {
		if (a != null) {
			return a.compareTo(b) < 0;
		}
		return b != null;
	}

	public final static boolean gt(String a, String b) {
		if (a != null) {
			return a.compareTo(b) > 0;
		}
		return false;
	}

	public final static boolean lte(String a, String b) {
		if (a != null) {
			return a.compareTo(b) <= 0;
		}
		return true;
	}

	public final static boolean gte(String a, String b) {
		if (a != null) {
			return a.compareTo(b) >= 0;
		}
		return b == null;
	}

	public final static int toint(String x) {
		try {
			return Integer.parseInt(x);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public final static long tolong(String x) {
		try {
			return Long.parseLong(x);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public final static double todouble(String x) {
		try {
			return Double.parseDouble(x);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public final static void p(String a) {
		System.out.println(a);
	}

	public final static String join(String[] a) {
		StringBuilder sb = new StringBuilder();
		for (String s : a) {
			sb.append(s);
		}
		return sb.toString();
	}

	/* Char */

	public static final boolean eq(char a, char b) {
		return a == b;
	}

	public static final boolean ne(char a, char b) {
		return a != b;
	}

	public final static boolean lt(char a, char b) {
		return a < b;
	}

	public final static boolean gt(char a, char b) {
		return a > b;
	}

	public final static boolean lte(char a, char b) {
		return a <= b;
	}

	public final static boolean gte(char a, char b) {
		return a >= b;
	}

	public final static int toint(char x) {
		return x;
	}

	public final static String toString(char x) {
		return String.valueOf(x);
	}

	// Data

	public final static List$ array(Object[] values) {
		return new List$(values);
	}

	public final static List$Int array(int[] values) {
		return new List$Int(values);
	}

	public final static List$Int range(int start, int end) {
		return new Range$Int(start, end);
	}

	public final static List$Float array(double[] values) {
		return new List$Float(values);
	}

	// Option

	public final static Object nop(Object o) {
		return o;
	}

	public final static Object none(String msg) {
		return new NullPointerException(msg);
	}

	public final static boolean isSome(Object o) {
		return !(o instanceof RuntimeException);
	}

	public final static boolean isNone(Object o) {
		return o instanceof RuntimeException;
	}

	public final static Object unwrap(Object o) {
		if (o instanceof RuntimeException) {
			throw (RuntimeException) o;
		}
		return o;
	}

	public final static boolean unwrapZ(Object o) {
		if (o instanceof RuntimeException) {
			throw (RuntimeException) o;
		}
		return unboxZ(o);
	}

	public final static int unwrapI(Object o) {
		if (o instanceof RuntimeException) {
			throw (RuntimeException) o;
		}
		return unboxI(o);
	}

	public final static double unwrapD(Object o) {
		if (o instanceof RuntimeException) {
			throw (RuntimeException) o;
		}
		return unboxD(o);
	}

	public final static void forEach(Object o, FuncObjVoid f) {
		if (isSome(o)) {
			f.apply(o);
		}
	}

	public final static Object map(Object o, FuncObjObj f) {
		if (isSome(o)) {
			return f.apply(o);
		}
		return null;
	}

	public final static Object flatMap(Object o, FuncObjObj f) {
		if (isSome(o)) {
			return f.apply(o);
		}
		return null;
	}

}

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

package blue.origami.rule;

import java.lang.reflect.Array;
//
//import origami.nez.iroha.api.IMethod;
import java.util.Objects;

import blue.origami.ffi.OAlias;
import blue.origami.ffi.OCast;

public class OrigamiAPIs {
	/* Object */

	// @ODynamic
	@OAlias(name = "==")
	public final static boolean _eq(Object a, Object b) {
		return Objects.equals(a, b);
	}

	// @ODynamic
	@OAlias(name = "!=")
	public final static boolean _ne(Object a, Object b) {
		return !Objects.equals(a, b);
	}

	// @ODynamic
	@OAlias(name = "size")
	public final static int size(Object a) {
		if (a == null) {
			return 0;
		}
		return (a.getClass().isArray()) ? Array.getLength(a) : 1;
	}

	/* Object, downcast */

	@OCast(cost = OCast.ANYCAST)
	public final static boolean toboolean(Object a) {
		return (Boolean) a;
	}

	@OCast(cost = OCast.ANYCAST)
	public final static byte tobyte(Object a) {
		return ((Number) a).byteValue();
	}

	@OCast(cost = OCast.ANYCAST)
	public final static short toshort(Object a) {
		return ((Number) a).shortValue();
	}

	@OCast(cost = OCast.ANYCAST)
	public final static int toint(Object a) {
		return ((Number) a).intValue();
	}

	@OCast(cost = OCast.ANYCAST)
	public final static long tolong(Object a) {
		return ((Number) a).longValue();
	}

	@OCast(cost = OCast.ANYCAST)
	public final static float tofloat(Object a) {
		return ((Number) a).floatValue();
	}

	@OCast(cost = OCast.ANYCAST)
	public final static double todouble(Object a) {
		return ((Number) a).doubleValue();
	}

	@OAlias(name = "!")
	public final static boolean _not(boolean a) {
		return !a;
	}

	@OAlias(name = "==")
	public final static boolean _eq(boolean a, boolean b) {
		return a == b;
	}

	@OAlias(name = "!=")
	public final static boolean _ne(boolean a, boolean b) {
		return a != b;
	}

	@OAlias(name = "&&")
	public final static boolean _and(boolean a, boolean b) {
		return a && b;
	}

	@OAlias(name = "||")
	public final static boolean _or(boolean a, boolean b) {
		return a || b;
	}

	/* Boolean, conversion */

	@OCast(cost = OCast.UPCAST)
	public final static Object toObject(boolean a) {
		return a;
	}

	@OCast(cost = OCast.SAME)
	public final static Boolean toBoolean(boolean a) {
		return a;
	}

	@OCast(cost = OCast.SAME)
	public final static boolean toboolean(Boolean a) {
		return a;
	}

	@OCast(cost = OCast.CONV)
	public final static String toString(boolean a) {
		return String.valueOf(a);
	}

	/* int */

	@OAlias(name = "-")
	public final static int _neq(int a) {
		return -a;
	}

	@OAlias(name = "+")
	public final static int _add(int a, int b) {
		return a + b;
	}

	@OAlias(name = "+")
	public final static String _add(int a, String b) {
		return a + b;
	}

	@OAlias(name = "-")
	public final static int _sub(int a, int b) {
		return a - b;
	}

	@OAlias(name = "*")
	public final static int _mul(int a, int b) {
		return a * b;
	}

	@OAlias(name = "/")
	public final static int _div(int a, int b) {
		return a / b;
	}

	@OAlias(name = "%")
	public final static int _mod(int a, int b) {
		return a % b;
	}

	@OAlias(name = "<>")
	public final static int compareTo(int a, int b) {
		return Integer.compare(a, b);
	}

	@OAlias(name = "==")
	public final static boolean _eq(int a, int b) {
		return a == b;
	}

	@OAlias(name = "!=")
	public final static boolean _ne(int a, int b) {
		return a != b;
	}

	@OAlias(name = "<")
	public final static boolean _lt(int a, int b) {
		return a < b;
	}

	@OAlias(name = ">")
	public final static boolean _gt(int a, int b) {
		return a > b;
	}

	@OAlias(name = "<=")
	public final static boolean _lte(int a, int b) {
		return a <= b;
	}

	@OAlias(name = ">=")
	public final static boolean _gte(int a, int b) {
		return a >= b;
	}

	@OAlias(name = "<<")
	public final static int shiftLeft(int a, int b) {
		return a << b;
	}

	@OAlias(name = ">>")
	public final static int shiftRight(int a, int b) {
		return a >> b;
	}

	@OAlias(name = ">>>")
	public final static int logicalRightShift(int a, int b) {
		return a >>> b;
	}

	@OAlias(name = "&")
	public final static int and(int a, int b) {
		return a & b;
	}

	@OAlias(name = "|")
	public final static int or(int a, int b) {
		return a | b;
	}

	@OAlias(name = "^")
	public final static int xor(int a, int b) {
		return a ^ b;
	}

	@OAlias(name = "~")
	public final static int not(int a) {
		return ~a;
	}

	/* int, conversion */

	@OCast(cost = OCast.LESSSAME)
	public final static byte tobyte(int a) {
		return (byte) a;
	}

	@OCast(cost = OCast.LESSSAME)
	public final static char tochar(int a) {
		return (char) a;
	}

	@OCast(cost = OCast.LESSSAME)
	public final static short to_short(int a) {
		return (short) a;
	}

	@OCast(cost = OCast.SAME)
	public final static long tolong(int a) {
		return a;
	}

	@OCast(cost = OCast.SAME)
	public final static float tofloat(int a) {
		return a;
	}

	@OCast(cost = OCast.SAME)
	public final static double todouble(int a) {
		return a;
	}

	@OCast(cost = OCast.UPCAST)
	public final static Object toObject(int a) {
		return a;
	}

	@OCast(cost = OCast.UPCAST)
	public final static Number toNumber(int a) {
		return a;
	}

	@OCast(cost = OCast.SAME)
	public final static Integer toInteger(int a) {
		return a;
	}

	@OCast(cost = OCast.CONV)
	public final static String toString(int a) {
		return String.valueOf(a);
	}

	@OCast(cost = OCast.SAME)
	public final static int toint(Integer a) {
		return a;
	}

	/* float conversion */

	@OCast(cost = OCast.LESSSAME)
	public final static byte tobyte(float a) {
		return (byte) a;
	}

	@OCast(cost = OCast.LESSSAME)
	public final static char tochar(float a) {
		return (char) a;
	}

	@OCast(cost = OCast.LESSSAME)
	public final static short toshort(float a) {
		return (short) a;
	}

	@OCast(cost = OCast.LESSSAME)
	public final static int toint(float a) {
		return (int) a;
	}

	@OCast(cost = OCast.LESSSAME)
	public final static long tolong(float a) {
		return (long) a;
	}

	@OCast(cost = OCast.SAME)
	public final static double todouble(float a) {
		return a;
	}

	@OCast(cost = OCast.UPCAST)
	public final static Object toObject(float a) {
		return a;
	}

	@OCast(cost = OCast.UPCAST)
	public final static Number toNumber(float a) {
		return a;
	}

	@OCast(cost = OCast.SAME)
	public final static Float toFloat(float a) {
		return a;
	}

	@OCast(cost = OCast.CONV)
	public final static String toString(float a) {
		return String.valueOf(a);
	}

	@OCast(cost = OCast.SAME)
	public final static float tofloat(float a) {
		return a;
	}

	@OAlias(name = "-")
	public final static long _neq(long a) {
		return -a;
	}

	@OAlias(name = "+")
	public final static long _add(long a, long b) {
		return a + b;
	}

	@OAlias(name = "+")
	public final static String _add(long a, String b) {
		return a + b;
	}

	@OAlias(name = "-")
	public final static long _sub(long a, long b) {
		return a - b;
	}

	@OAlias(name = "*")
	public final static long _mul(long a, long b) {
		return a * b;
	}

	@OAlias(name = "*")
	public final static long _div(long a, long b) {
		return a / b;
	}

	@OAlias(name = "%")
	public final static long _mod(long a, long b) {
		return a % b;
	}

	@OAlias(name = "<>")
	public final static int compareTo(long a, long b) {
		return Long.compare(a, b);
	}

	@OAlias(name = "==")
	public final static boolean _eq(long a, long b) {
		return a == b;
	}

	@OAlias(name = "!=")
	public final static boolean _ne(long a, long b) {
		return a != b;
	}

	@OAlias(name = "<")
	public final static boolean _lt(long a, long b) {
		return a < b;
	}

	@OAlias(name = ">")
	public final static boolean _gt(long a, long b) {
		return a > b;
	}

	@OAlias(name = "<=")
	public final static boolean _lte(long a, long b) {
		return a <= b;
	}

	@OAlias(name = ">=")
	public final static boolean _gte(long a, long b) {
		return a >= b;
	}

	@OAlias(name = "<<")
	public final static long shiftLeft(long a, long b) {
		return a << b;
	}

	@OAlias(name = ">>")
	public final static long shiftRight(long a, long b) {
		return a >> b;
	}

	@OAlias(name = ">>>")
	public final static long opLogicalRightShift(long a, long b) {
		return a >>> b;
	}

	@OAlias(name = "&")
	public final static long and(long a, long b) {
		return a & b;
	}

	@OAlias(name = "|")
	public final static long or(long a, long b) {
		return a | b;
	}

	@OAlias(name = "^")
	public final static long xor(long a, long b) {
		return a ^ b;
	}

	@OAlias(name = "~")
	public final static long not(long a) {
		return ~a;
	}

	/* cast */

	@OCast(cost = OCast.LESSSAME)
	public final static byte tobyte(long a) {
		return (byte) a;
	}

	@OCast(cost = OCast.LESSSAME)
	public final static char tochar(long a) {
		return (char) a;
	}

	@OCast(cost = OCast.LESSSAME)
	public final static short toshort(long a) {
		return (short) a;
	}

	@OCast(cost = OCast.LESSSAME)
	public final static int toint(long a) {
		return (int) a;
	}

	@OCast(cost = OCast.LESSSAME)
	public final static float tofloat(long a) {
		return a;
	}

	@OCast(cost = OCast.SAME)
	public final static double todouble(long a) {
		return a;
	}

	@OCast(cost = OCast.UPCAST)
	public final static Object toObject(long a) {
		return a;
	}

	@OCast(cost = OCast.UPCAST)
	public final static Number toNumber(long a) {
		return a;
	}

	@OCast(cost = OCast.SAME)
	public final static Long toLong(long a) {
		return a;
	}

	@OCast(cost = OCast.CONV)
	public final static String toString(long a) {
		return String.valueOf(a);
	}

	@OCast(cost = OCast.SAME)
	public final static long tolong(Long a) {
		return a;
	}

	/* Double */

	@OAlias(name = "-")
	public final static double _neq(double a) {
		return -a;
	}

	@OAlias(name = "+")
	public final static double _add(double a, double b) {
		return a + b;
	}

	@OAlias(name = "+")
	public final static String _add(double a, String b) {
		return a + b;
	}

	@OAlias(name = "-")
	public final static double _sub(double a, double b) {
		return a - b;
	}

	@OAlias(name = "*")
	public final static double _mul(double a, double b) {
		return a * b;
	}

	@OAlias(name = "/")
	public final static double _div(double a, double b) {
		return a / b;
	}

	@OAlias(name = "%")
	public final static double _mod(double a, double b) {
		return a % b;
	}

	@OAlias(name = "<>")
	public final static int compareTo(double a, double b) {
		return Double.compare(a, b);
	}

	@OAlias(name = "==")
	public final static boolean _eq(double a, double b) {
		return a == b;
	}

	@OAlias(name = "!=")
	public final static boolean _ne(double a, double b) {
		return a != b;
	}

	@OAlias(name = "<")
	public final static boolean _lt(double a, double b) {
		return a < b;
	}

	@OAlias(name = ">")
	public final static boolean _gt(double a, double b) {
		return a > b;
	}

	@OAlias(name = "<=")
	public final static boolean _lte(double a, double b) {
		return a <= b;
	}

	@OAlias(name = ">=")
	public final static boolean _gte(double a, double b) {
		return a >= b;
	}

	/* cast */

	@OCast(cost = OCast.CONV)
	public final static byte tobyte(double a) {
		return (byte) a;
	}

	@OCast(cost = OCast.CONV)
	public final static char tochar(double a) {
		return (char) a;
	}

	@OCast(cost = OCast.CONV)
	public final static short toshort(double a) {
		return (short) a;
	}

	@OCast(cost = OCast.LESSSAME)
	public final static int toint(double a) {
		return (int) a;
	}

	@OCast(cost = OCast.LESSSAME)
	public final static long tolong(double a) {
		return (long) a;
	}

	@OCast(cost = OCast.LESSSAME)
	public final static float tofloat(double a) {
		return (float) a;
	}

	/* Conversion */

	@OCast(cost = OCast.UPCAST)
	public final static Object toObject(double a) {
		return a;
	}

	@OCast(cost = OCast.UPCAST)
	public final static Number toNumber(double a) {
		return a;
	}

	@OCast(cost = OCast.SAME)
	public final static Double toDouble(double a) {
		return a;
	}

	@OCast(cost = OCast.CONV)
	public final static String toString(double a) {
		return String.valueOf(a);
	}

	@OCast(cost = OCast.SAME)
	public final static double todouble(Double a) {
		return a;
	}

	@OCast(cost = OCast.SAME)
	public final static double todouble(Float a) {
		return a;
	}

	@OCast(cost = OCast.SAME)
	public final static double todouble(Long a) {
		return a;
	}

	@OCast(cost = OCast.SAME)
	public final static double todouble(Integer a) {
		return a;
	}

	/* Object[] */

	public final static int size(Object[] a) {
		return a.length;
	}

	public final static boolean get(boolean[] a, int index) {
		return a[index];
	}

	public final static void set(boolean[] a, int index, boolean v) {
		a[index] = v;
	}

	public final static int get(int[] a, int index) {
		return a[index];
	}

	public final static void set(int[] a, int index, int v) {
		a[index] = v;
	}

	public final static long get(long[] a, int index) {
		return a[index];
	}

	public final static void set(long[] a, int index, long v) {
		a[index] = v;
	}

	public final static float get(float[] a, int index) {
		return a[index];
	}

	public final static void set(float[] a, int index, float v) {
		a[index] = v;
	}

	public final static double get(double[] a, int index) {
		return a[index];
	}

	public final static void set(double[] a, int index, double v) {
		a[index] = v;
	}

	public final static <T> T get(T[] a, int index) {
		return a[index];
	}

	public final static <T> void set(T[] a, int index, T v) {
		a[index] = v;
	}

	/* String */

	@OAlias(name = "size")
	public static final int size(String x) {
		return x.length();
	}

	public static final String get(String x, int n) {
		return String.valueOf(x.charAt(n));
	}

	@OAlias(name = "+")
	public static final String _add(String x, String y) {
		return x + y;
	}

	@OAlias(name = "<>")
	public static final int compareTo(String x, String y) {
		return x.compareTo(y);
	}

	@OAlias(name = "==")
	public static final boolean _eq(String x, String y) {
		if (x == null || y == null) {
			return x == y;
		}
		return x.equals(y);
	}

	@OAlias(name = "!=")
	public static final boolean _ne(String x, String y) {
		if (x == null || y == null) {
			return x != y;
		}
		return !x.equals(y);
	}

	@OAlias(name = "<")
	public final static boolean _lt(String a, String b) {
		if (a != null) {
			return a.compareTo(b) < 0;
		}
		return b != null;
	}

	@OAlias(name = ">")
	public final static boolean _gt(String a, String b) {
		if (a != null) {
			return a.compareTo(b) > 0;
		}
		return false;
	}

	@OAlias(name = "<=")
	public final static boolean _lte(String a, String b) {
		if (a != null) {
			return a.compareTo(b) <= 0;
		}
		return true;
	}

	@OAlias(name = ">=")
	public final static boolean _gte(String a, String b) {
		if (a != null) {
			return a.compareTo(b) >= 0;
		}
		return b == null;
	}

	@OCast(cost = OCast.LESSCONV)
	public final static int toint(String x) {
		try {
			return Integer.parseInt(x);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	@OCast(cost = OCast.LESSCONV)
	public final static long tolong(String x) {
		try {
			return Long.parseLong(x);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	@OCast(cost = OCast.LESSCONV)
	public final static double todouble(String x) {
		try {
			return Double.parseDouble(x);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	// array to elements

	@OCast(cost = OCast.LESSCONV)
	public static boolean toboolean(boolean[] a) {
		if (a.length > 0) {
			return a[0];
		}
		return false;
	}

	@OCast(cost = OCast.LESSCONV)
	public static int toint(int[] a) {
		if (a.length > 0) {
			return a[0];
		}
		return 0;
	}

	@OCast(cost = OCast.LESSCONV)
	public static double todouble(double[] a) {
		if (a.length > 0) {
			return a[0];
		}
		return 0.0;
	}

	// value to array

	@OCast(cost = OCast.CONV)
	public static boolean[] tobool(boolean v) {
		return new boolean[] { v };
	}

	@OCast(cost = OCast.CONV)
	public static int[] toint(int v) {
		return new int[] { v };
	}

	@OCast(cost = OCast.CONV)
	public static double[] todouble(double v) {
		return new double[] { v };
	}

	@OCast(cost = OCast.CONV)
	public static String[] toS(String v) {
		return new String[] { v };
	}

}

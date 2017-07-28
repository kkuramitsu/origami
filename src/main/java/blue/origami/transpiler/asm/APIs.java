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

package blue.origami.transpiler.asm;

import java.util.Objects;

import blue.origami.ffi.OAlias;
import blue.origami.ffi.OCast;

public class APIs {
	// /* Object */
	//
	// // @ODynamic
	// @OAlias(name = "==")
	// public final static boolean eq(Object a, Object b) {
	// return Objects.equals(a, b);
	// }
	//
	// // @ODynamic
	// @OAlias(name = "!=")
	// public final static boolean ne(Object a, Object b) {
	// return !Objects.equals(a, b);
	// }
	//
	// // @ODynamic
	// @OAlias(name = "size")
	// public final static int size(Object a) {
	// if (a == null) {
	// return 0;
	// }
	// return (a.getClass().isArray()) ? Array.getLength(a) : 1;
	// }

	/* Object, downcast */
	//
	// @OCast(cost = OCast.ANYCAST)
	// public final static boolean toboolean(Object a) {
	// return (Boolean) a;
	// }
	//
	// @OCast(cost = OCast.ANYCAST)
	// public final static byte tobyte(Object a) {
	// return ((Number) a).byteValue();
	// }
	//
	// @OCast(cost = OCast.ANYCAST)
	// public final static short toshort(Object a) {
	// return ((Number) a).shortValue();
	// }
	//
	// @OCast(cost = OCast.ANYCAST)
	// public final static int toint(Object a) {
	// return ((Number) a).intValue();
	// }
	//
	// @OCast(cost = OCast.ANYCAST)
	// public final static long tolong(Object a) {
	// return ((Number) a).longValue();
	// }
	//
	// @OCast(cost = OCast.ANYCAST)
	// public final static float tofloat(Object a) {
	// return ((Number) a).floatValue();
	// }
	//
	// @OCast(cost = OCast.ANYCAST)
	// public final static double todouble(Object a) {
	// return ((Number) a).doubleValue();
	// }

	@OAlias(name = "!")
	public final static boolean not(boolean a) {
		return !a;
	}

	@OAlias(name = "==")
	public final static boolean eq(boolean a, boolean b) {
		return a == b;
	}

	@OAlias(name = "!=")
	public final static boolean ne(boolean a, boolean b) {
		return a != b;
	}

	public final static boolean _assert(boolean a) {
		assert (a);
		return a;
	}

	/* Boolean, conversion */

	public final static Boolean box(boolean a) {
		return a;
	}

	public final static boolean unbox(Boolean a) {
		return a;
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

	/* int, conversion */

	public final static byte tobyte(int a) {
		return (byte) a;
	}

	public final static char tochar(int a) {
		return (char) a;
	}

	public final static short toshort(int a) {
		return (short) a;
	}

	public final static Integer box(int a) {
		return a;
	}

	public final static int unbox(Integer a) {
		return a;
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

	public final static Double box(double a) {
		return a;
	}

	public final static String toString(double a) {
		return String.valueOf(a);
	}

	public final static double unbox(Double a) {
		return a;
	}

	/* String */

	public static final int size(String x) {
		return x == null ? 0 : x.length();
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

	public final static String join(String[] a) {
		StringBuilder sb = new StringBuilder();
		for (String s : a) {
			sb.append(s);
		}
		return sb.toString();
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

	// /* float conversion */
	//
	// @OCast(cost = OCast.LESSSAME)
	// public final static byte tobyte(float a) {
	// return (byte) a;
	// }
	//
	// @OCast(cost = OCast.LESSSAME)
	// public final static char tochar(float a) {
	// return (char) a;
	// }
	//
	// @OCast(cost = OCast.LESSSAME)
	// public final static short toshort(float a) {
	// return (short) a;
	// }
	//
	// @OCast(cost = OCast.LESSSAME)
	// public final static int toint(float a) {
	// return (int) a;
	// }
	//
	// @OCast(cost = OCast.LESSSAME)
	// public final static long tolong(float a) {
	// return (long) a;
	// }
	//
	// @OCast(cost = OCast.SAME)
	// public final static double todouble(float a) {
	// return a;
	// }
	//
	// @OCast(cost = OCast.UPCAST)
	// public final static Object toObject(float a) {
	// return a;
	// }
	//
	// @OCast(cost = OCast.UPCAST)
	// public final static Number toNumber(float a) {
	// return a;
	// }
	//
	// @OCast(cost = OCast.SAME)
	// public final static Float toFloat(float a) {
	// return a;
	// }
	//
	// @OCast(cost = OCast.CONV)
	// public final static String toString(float a) {
	// return String.valueOf(a);
	// }
	//
	// @OCast(cost = OCast.SAME)
	// public final static float tofloat(float a) {
	// return a;
	// }
	//
	// @OAlias(name = "-")
	// public final static long neq(long a) {
	// return -a;
	// }
	//
	// @OAlias(name = "+")
	// public final static long add(long a, long b) {
	// return a + b;
	// }
	//
	// @OAlias(name = "+")
	// public final static String add(long a, String b) {
	// return a + b;
	// }
	//
	// @OAlias(name = "-")
	// public final static long _sub(long a, long b) {
	// return a - b;
	// }
	//
	// @OAlias(name = "*")
	// public final static long _mul(long a, long b) {
	// return a * b;
	// }
	//
	// @OAlias(name = "*")
	// public final static long _div(long a, long b) {
	// return a / b;
	// }
	//
	// @OAlias(name = "%")
	// public final static long _mod(long a, long b) {
	// return a % b;
	// }
	//
	// @OAlias(name = "<>")
	// public final static int compareTo(long a, long b) {
	// return Long.compare(a, b);
	// }
	//
	// @OAlias(name = "==")
	// public final static boolean eq(long a, long b) {
	// return a == b;
	// }
	//
	// @OAlias(name = "!=")
	// public final static boolean ne(long a, long b) {
	// return a != b;
	// }
	//
	// @OAlias(name = "<")
	// public final static boolean lt(long a, long b) {
	// return a < b;
	// }
	//
	// @OAlias(name = ">")
	// public final static boolean gt(long a, long b) {
	// return a > b;
	// }
	//
	// @OAlias(name = "<=")
	// public final static boolean lte(long a, long b) {
	// return a <= b;
	// }
	//
	// @OAlias(name = ">=")
	// public final static boolean gte(long a, long b) {
	// return a >= b;
	// }
	//
	// @OAlias(name = "<<")
	// public final static long shiftLeft(long a, long b) {
	// return a << b;
	// }
	//
	// @OAlias(name = ">>")
	// public final static long shiftRight(long a, long b) {
	// return a >> b;
	// }
	//
	// @OAlias(name = ">>>")
	// public final static long opLogicalRightShift(long a, long b) {
	// return a >>> b;
	// }
	//
	// @OAlias(name = "&")
	// public final static long and(long a, long b) {
	// return a & b;
	// }
	//
	// @OAlias(name = "|")
	// public final static long or(long a, long b) {
	// return a | b;
	// }
	//
	// @OAlias(name = "^")
	// public final static long xor(long a, long b) {
	// return a ^ b;
	// }
	//
	// @OAlias(name = "~")
	// public final static long not(long a) {
	// return ~a;
	// }
	//
	// /* cast */
	//
	// @OCast(cost = OCast.LESSSAME)
	// public final static byte tobyte(long a) {
	// return (byte) a;
	// }
	//
	// @OCast(cost = OCast.LESSSAME)
	// public final static char tochar(long a) {
	// return (char) a;
	// }
	//
	// @OCast(cost = OCast.LESSSAME)
	// public final static short toshort(long a) {
	// return (short) a;
	// }
	//
	// @OCast(cost = OCast.LESSSAME)
	// public final static int toint(long a) {
	// return (int) a;
	// }
	//
	// @OCast(cost = OCast.LESSSAME)
	// public final static float tofloat(long a) {
	// return a;
	// }
	//
	// @OCast(cost = OCast.SAME)
	// public final static double todouble(long a) {
	// return a;
	// }
	//
	// @OCast(cost = OCast.UPCAST)
	// public final static Object toObject(long a) {
	// return a;
	// }
	//
	// @OCast(cost = OCast.UPCAST)
	// public final static Number toNumber(long a) {
	// return a;
	// }
	//
	// @OCast(cost = OCast.SAME)
	// public final static Long toLong(long a) {
	// return a;
	// }
	//
	// @OCast(cost = OCast.CONV)
	// public final static String toString(long a) {
	// return String.valueOf(a);
	// }
	//
	// @OCast(cost = OCast.SAME)
	// public final static long tolong(Long a) {
	// return a;
	// }

}

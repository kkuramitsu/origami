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
import blue.origami.konoha5.Data;
import blue.origami.konoha5.IntArray;
import blue.origami.konoha5.IntRange;
import blue.origami.konoha5.ObjArray;

public class APIs {
	public final static Boolean box(boolean a) {
		return a;
	}

	public final static boolean unboxZ(Object a) {
		return (Boolean) a;
	}

	public final static Integer box(int a) {
		return a;
	}

	public final static int unboxI(Object a) {
		return ((Number) a).intValue();
	}

	public final static Double box(double a) {
		return a;
	}

	public final static double unboxD(Object a) {
		return ((Number) a).doubleValue();
	}

	public final static String unboxS(Object a) {
		return (String) a;
	}

	// public final static Long box(long a) {
	// return a;
	// }
	//
	// public final static long unboxL(Object a) {
	// return ((Number) a).longValue();
	// }

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

	public final static boolean p(String a) {
		System.out.println(a);
		return true;
	}

	public final static String join(String[] a) {
		StringBuilder sb = new StringBuilder();
		for (String s : a) {
			sb.append(s);
		}
		return sb.toString();
	}

	// Data

	public final static ObjArray array(Object[] values) {
		return new ObjArray(values, values.length);
	}

	public final static IntArray array(int[] values) {
		return new IntArray(values, values.length);
	}

	public final static IntArray range(int start, int end) {
		return new IntRange(start, end);
	}

	public final static Data data(int[] keys, Object[] values) {
		return new Data(keys, values);
	}

	public final static int getf(Data d, int key, int def) {
		Object v = d.getf(key, null);
		return v == null ? def : ((Number) v).intValue();
	}

	public final static Data setf(Data d, int key, int def) {
		d.setf(key, def);
		return d;
	}

	public final static double getf(Data d, int key, double def) {
		Object v = d.getf(key, null);
		return v == null ? def : ((Number) v).doubleValue();
	}

	public final static Data setf(Data d, int key, double def) {
		d.setf(key, def);
		return d;
	}

	public final static String getf(Data d, int key, String def) {
		Object v = d.getf(key, null);
		return v == null ? def : (String) v;
	}

	public final static Data setf(Data d, int key, String def) {
		d.setf(key, def);
		return d;
	}

	public final static Data getf(Data d, int key, Data def) {
		Object v = d.getf(key, null);
		return v == null ? def : (Data) v;
	}

	public final static Data setf(Data d, int key, Data def) {
		d.setf(key, def);
		return d;
	}

}

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

package blue.origami.rule.js;

import blue.origami.ffi.OAlias;

public class JsCore {

	@OAlias(name = "+")
	public final static Object add(Object a, Object b) {
		if (Number.class.isInstance(a) && Number.class.isInstance(b)) {
			Double a1 = Number.class.cast(a).doubleValue();
			Double b1 = Number.class.cast(b).doubleValue();
			return a1 + b1;
		}
		String a1 = a.toString();
		String b1 = b.toString();
		return a1 + b1;
	}

	@OAlias(name = "-")
	public final static Object subtract(Object a, Object b) {
		if (Number.class.isInstance(a) && Number.class.isInstance(b)) {
			Double a1 = Number.class.cast(a).doubleValue();
			Double b1 = Number.class.cast(b).doubleValue();
			return a1 - b1;
		}
		String a1 = a.toString();
		String b1 = b.toString();
		try {
			Double a2 = Double.parseDouble(a1);
			Double b2 = Double.parseDouble(b1);
			return a2 - b2;
		} catch (NumberFormatException e) {
		}
		throw new RuntimeException();
	}

	@OAlias(name = "*")
	public final static Object multiply(Object a, Object b) {
		if (Number.class.isInstance(a) && Number.class.isInstance(b)) {
			Double a1 = Number.class.cast(a).doubleValue();
			Double b1 = Number.class.cast(b).doubleValue();
			return a1 * b1;
		}
		String a1 = a.toString();
		String b1 = b.toString();
		try {
			Double a2 = Double.parseDouble(a1);
			Double b2 = Double.parseDouble(b1);
			return a2 * b2;
		} catch (NumberFormatException e) {
		}
		throw new RuntimeException();
	}

	@OAlias(name = "/")
	public final static Object divide(Object a, Object b) {
		if (Number.class.isInstance(a) && Number.class.isInstance(b)) {
			Double a1 = Number.class.cast(a).doubleValue();
			Double b1 = Number.class.cast(b).doubleValue();
			return a1 / b1;
		}
		String a1 = a.toString();
		String b1 = b.toString();
		try {
			Double a2 = Double.parseDouble(a1);
			Double b2 = Double.parseDouble(b1);
			return a2 / b2;
		} catch (NumberFormatException e) {
		}
		throw new RuntimeException();
	}

	@OAlias(name = "%")
	public final static Object mod(Object a, Object b) {
		if (Number.class.isInstance(a) && Number.class.isInstance(b)) {
			Double a1 = Number.class.cast(a).doubleValue();
			Double b1 = Number.class.cast(b).doubleValue();
			return a1 % b1;
		}
		String a1 = a.toString();
		String b1 = b.toString();
		try {
			Double a2 = Double.parseDouble(a1);
			Double b2 = Double.parseDouble(b1);
			return a2 % b2;
		} catch (NumberFormatException e) {
		}
		throw new RuntimeException();
	}

	@OAlias(name = "<")
	public static Boolean lt(Object a, Object b) {
		return compareTo(a, b) < 0;
	}

	@OAlias(name = "<=")
	public static Boolean lte(Object a, Object b) {
		return compareTo(a, b) <= 0;
	}

	@OAlias(name = ">")
	public static Boolean gt(Object a, Object b) {
		return compareTo(a, b) > 0;
	}

	@OAlias(name = ">=")
	public static Boolean gte(Object a, Object b) {
		return compareTo(a, b) >= 0;
	}

	public static double compareTo(Object a, Object b) {
		if (Number.class.isInstance(a) && Number.class.isInstance(b)) {
			Double a1 = Number.class.cast(a).doubleValue();
			Double b1 = Number.class.cast(b).doubleValue();
			return a1 - b1;
		}
		String a1 = a.toString();
		String b1 = b.toString();
		return a1.compareTo(b1);
	}
}

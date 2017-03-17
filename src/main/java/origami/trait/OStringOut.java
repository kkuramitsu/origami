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

package origami.trait;

import java.lang.reflect.Field;

import origami.ODebug;
import origami.ffi.OAlias;

public interface OStringOut {
	// default method cannot override a method from java.lang.Object
	// public default String toString() {
	// StringBuilder sb = new StringBuilder();
	// strOut(sb);
	// return sb.toString();
	// }

	public void strOut(StringBuilder sb);

	public static String stringfy(OStringOut o) {
		StringBuilder sb = new StringBuilder();
		append(sb, o);
		return sb.toString();
	}

	public static void append(StringBuilder sb, Object o) {
		if (o instanceof OStringOut) {
			((OStringOut) o).strOut(sb);
		} else {
			sb.append(o);
		}
	}

	public static void appendQuoted(StringBuilder sb, Object o) {
		if (o instanceof OStringOut) {
			((OStringOut) o).strOut(sb);
		} else if (o instanceof String) {
			sb.append("'");
			sb.append(o);
			sb.append("'");
		} else {
			sb.append(o);
		}
	}

	public static int appendField(StringBuilder sb, Object o, Class<?> untilClass, int cnt) {
		if (o == null) {
			return cnt;
		}
		for (Class<?> c = o.getClass(); c != untilClass; c = c.getSuperclass()) {
			Field[] fs = c.getDeclaredFields();
			for (Field f : fs) {
				if (OTypeUtils.isPublic(f) && !OTypeUtils.isStatic(f)) {
					if (cnt > 0) {
						sb.append(", ");
					}
					OAlias a = f.getAnnotation(OAlias.class);
					String name = a == null ? f.getName() : a.name();
					sb.append(name);
					sb.append(": ");
					OStringOut.appendQuoted(sb, OTypeUtils.fieldValue(f, o));
					cnt++;
				}
			}
		}
		return cnt;
	}

	public static String format(String fmt, Object... args) {
		assert (fmt != null);
		try {
			return String.format(fmt, args);
		} catch (Exception e) {
			ODebug.traceException(e);
			return "FIXME: WRONG FORMAT: '" + fmt + "' by " + e;
		}
	}

}

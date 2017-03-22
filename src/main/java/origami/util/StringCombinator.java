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

package origami.util;

import java.lang.reflect.Field;

import origami.ODebug;
import origami.ffi.OAlias;
import origami.nez.ast.LocaleFormat;

public interface StringCombinator {

	public void strOut(StringBuilder sb);

	public static String stringfy(StringCombinator o) {
		StringBuilder sb = new StringBuilder();
		append(sb, o);
		return sb.toString();
	}

	public static void append(StringBuilder sb, Object o) {
		if (o instanceof StringCombinator) {
			((StringCombinator) o).strOut(sb);
		} else {
			sb.append(o);
		}
	}

	public static void appendQuoted(StringBuilder sb, Object o) {
		if (o instanceof StringCombinator) {
			((StringCombinator) o).strOut(sb);
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
					StringCombinator.appendQuoted(sb, OTypeUtils.fieldValue(f, o));
					cnt++;
				}
			}
		}
		return cnt;
	}

	public static String format(LocaleFormat fmt, Object... args) {
		return format(fmt.toString(), args);
	}

	public static String format(String fmt, Object... args) {
		StringBuilder sb = new StringBuilder();
		appendFormat(sb, fmt, args);
		return sb.toString();
	}

	public static void appendFormat(StringBuilder sb, LocaleFormat fmt, Object... args) {
		appendFormat(sb, fmt.toString(), args);
	}

	public static void appendFormat(StringBuilder sb, String fmt, Object... args) {
		assert (fmt != null);
		try {
			if(fmt.indexOf("$0") >= 0) {
				formatIndexedFormat(sb, fmt, args);
			}
			else {
				sb.append(String.format(fmt, args));
			}
		} catch (Exception e) {
			ODebug.traceException(e);
			sb.append("FIXME: WRONG FORMAT: '" + fmt + "' by " + e);
		}
	}

	static void formatIndexedFormat(StringBuilder sb, String fmt2, Object[] args) {
		String[] tokens = fmt2.split("\\$", -1);
		sb.append(tokens[0]);
		for(int i = 1; i < tokens.length; i++) {
			String t = tokens[i];
			if(t.length() > 0 && Character.isDigit(t.charAt(0))) {
				int index = t.charAt(0) - '0';
				StringCombinator.append(sb, args[index]);
				sb.append(t.substring(1));
			}
			else {
				sb.append("$");
				sb.append(t);				
			}
		}
	}
	

}

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

package origami.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import origami.type.OType;

public class OAsmUtils {

	public static Type asmType(Class<?> c) {
		return Type.getType(c);
	}

	public static Type[] asmTypes(Class<?>... c) {
		Type[] t = new Type[c.length];
		for (int i = 0; i < t.length; i++) {
			t[i] = Type.getType(c[i]);
		}
		return t;
	}

	public static String getInternalName(Class<?> c) {
		return asmType(c).getInternalName();
	}

	public static Method asmMethod(Class<?> ret, String methodName, Class<?>... p) {
		return new Method(methodName, asmType(ret), asmTypes(p));
	}

	public static Type asmType(OType t) {
		return Type.getType(t.typeDesc(0));
	}

	public final static String descMethod(OType r, String methodName, OType... p) {
		StringBuilder sb = new StringBuilder();
		// sb.append(methodName);
		sb.append("(");
		int i = 0;
		for (OType t : p) {
			sb.append(t.getName());
			if (i < p.length - 1) {
				sb.append(",");
			}
			i++;
		}
		sb.append(")");
		return sb.toString();
	}

	public static final Method asmMethod(OType r, String methodName, OType... p) {
		StringBuilder sb = new StringBuilder();
		sb.append(r.getName());
		sb.append(" ");
		sb.append(methodName);
		sb.append("(");
		int c = 0;
		for (OType t : p) {
			if (c > 0) {
				sb.append(",");
			}
			sb.append(t.getName());
			c++;
		}
		sb.append(")");
		return Method.getMethod(sb.toString());
	}

	// public static final Method asmMethod2(OType r, String methodName,
	// OType... p) {
	// MethodDeclSignatureWriter methodSign = new MethodDeclSignatureWriter(r,
	// p);
	// // ODebug.trace("descMethod: %s%s", methodName, methodSign.getDesc());
	// return Method.getMethod(methodName + methodSign.getDesc(), false);
	//
	// }

	public static final Method asmMethod(java.lang.reflect.Method m) {
		return Method.getMethod(m);
	}

}

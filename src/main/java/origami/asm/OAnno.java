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

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import origami.ffi.OVirtual;
import origami.rule.OSymbols;

public class OAnno implements OSymbols {

	private HashMap<Class<?>, Map<String, Object>> annoMap = null;
	private boolean isReadOnly = false;
	private int acc = 0;

	public OAnno(int mod) {
		this.acc = mod;
	}

	public OAnno(String spec) {
		this(0);
		this.add(spec);
	}

	public void add(String spec) {
		String[] annos = spec.split(",");
		for (String s : annos) {
			s = s.toLowerCase();
			switch (s) {
			case "public":
				acc |= Opcodes.ACC_PUBLIC;
				acc &= ~Opcodes.ACC_PROTECTED;
				acc &= ~Opcodes.ACC_PRIVATE;
				break;
			case "protected":
				acc |= Opcodes.ACC_PROTECTED;
				acc &= ~Opcodes.ACC_PUBLIC;
				acc &= ~Opcodes.ACC_PRIVATE;
				break;
			case "private":
				acc |= Opcodes.ACC_PRIVATE;
				acc &= ~Opcodes.ACC_PUBLIC;
				acc &= ~Opcodes.ACC_PROTECTED;
				break;
			case "static":
				acc |= Opcodes.ACC_STATIC;
				break;
			case "abstract":
				acc |= Opcodes.ACC_ABSTRACT;
				break;
			case "interface":
				acc |= Opcodes.ACC_INTERFACE;
				break;
			case "final":
				acc |= Opcodes.ACC_FINAL;
				break;
			case "native":
				acc |= Opcodes.ACC_NATIVE;
				break;
			case "synchronized":
				acc |= Opcodes.ACC_SYNCHRONIZED;
				break;
			case "strictfp":
				acc |= Opcodes.ACC_STRICT;
				break;
			}
		}
	}

	public final int acc() {
		return acc;
	}

	public final boolean isPublic() {
		return ((acc & Opcodes.ACC_PUBLIC) == Opcodes.ACC_PUBLIC);
	}

	public final boolean isPrivate() {
		return ((acc & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE);
	}

	public final boolean isProtected() {
		return ((acc & Opcodes.ACC_PROTECTED) == Opcodes.ACC_PROTECTED);
	}

	public final boolean isInterface() {
		return ((acc & Opcodes.ACC_INTERFACE) == Opcodes.ACC_INTERFACE);
	}

	public final boolean isAbstract() {
		return ((acc & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT);
	}

	public final boolean isFinal() {
		return ((acc & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL);
	}

	public final boolean isReadOnly() {
		/* We use isReadOnly flag, because the final field cannot be changed */
		return isReadOnly || this.isFinal();
	}

	public final void setReadOnly(boolean b) {
		this.isReadOnly = b;
	}

	public final boolean isStatic() {
		return ((acc & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC);
	}

	public final boolean isVirtual() {
		return (annoMap != null && annoMap.containsKey(OVirtual.class));
	}

	public void setAnnotation(Class<?> c) {
		setAnnotation(c, null);
	}

	public void setAnnotation(Class<?> c, String key, Object value) {
		HashMap<String, Object> data = new HashMap<>();
		data.put(key, value);
		setAnnotation(c, data);
	}

	public void setAnnotation(Class<?> c, Map<String, Object> value) {
		assert (c.isAnnotation());
		if (annoMap == null) {
			annoMap = new HashMap<>();
		}
		annoMap.put(c, value);
	}

	// public void copyFrom(Class<?> c, OAnno anno) {
	// if (anno.annoMap != null && anno.annoMap.containsKey(c)) {
	// setAnnotation(c, anno.annoMap.get(c));
	// }
	// }

	void asm(Object cv) {
		if (annoMap != null) {
			for (Class<?> c : annoMap.keySet()) {
				AnnotationVisitor av = visitAnnotation(cv, c);
				Map<String, Object> values = annoMap.get(c);
				if (values != null) {
					for (String key : values.keySet()) {
						av.visit(key, values.get(key));
					}
				}
				av.visitEnd();
			}
		}
	}

	private static AnnotationVisitor visitAnnotation(Object v, Class<?> c) {
		String desc = "L" + c.getCanonicalName().replace('.', '/') + ";";
		if (v instanceof ClassVisitor) {
			return ((ClassVisitor) v).visitAnnotation(desc, true);
		}
		if (v instanceof MethodVisitor) {
			return ((MethodVisitor) v).visitAnnotation(desc, true);
		}
		if (v instanceof FieldVisitor) {
			return ((FieldVisitor) v).visitAnnotation(desc, true);
		}
		throw new RuntimeException("undefined visitor: " + v.getClass());
	}

	public static void asmAnnotation(Object v, Class<?> c) {
		AnnotationVisitor av = visitAnnotation(v, c);
		av.visitEnd();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (this.isPublic()) {
			sb.append(" public");
		}
		if (this.isFinal()) {
			sb.append(" final");
		}
		if (this.isStatic()) {
			sb.append(" static");
		}
		if (this.isVirtual()) {
			sb.append(" virtual");
		}
		return sb.toString();
	}
}

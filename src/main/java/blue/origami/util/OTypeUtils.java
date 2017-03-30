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

package blue.origami.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import blue.origami.code.OErrorCode;
import blue.origami.ffi.OCast;
import blue.origami.lang.OConstructor;
import blue.origami.lang.OMethod;
import blue.origami.lang.OMethodHandle;
import blue.origami.lang.OEnv.OListMatcher;
import blue.origami.lang.type.OType;
import blue.origami.lang.type.OTypeSystem;

public class OTypeUtils {

	public final static Class<?> unboxType(Class<?> c) {
		return unboxMap.getOrDefault(c, c);
	}

	public final static Class<?> boxType(Class<?> c) {
		return boxMap.getOrDefault(c, c);
	}

	private final static Map<Class<?>, Class<?>> boxMap = new HashMap<>();
	private final static Map<Class<?>, Class<?>> unboxMap = new HashMap<>();

	static {
		unboxMap.put(Boolean.class, boolean.class);
		unboxMap.put(Byte.class, byte.class);
		unboxMap.put(Character.class, char.class);
		unboxMap.put(Short.class, short.class);
		unboxMap.put(Integer.class, int.class);
		unboxMap.put(Float.class, float.class);
		unboxMap.put(Long.class, long.class);
		unboxMap.put(Double.class, double.class);
	}

	static {
		boxMap.put(boolean.class, Boolean.class);
		boxMap.put(byte.class, Byte.class);
		boxMap.put(char.class, Character.class);
		boxMap.put(short.class, Short.class);
		boxMap.put(int.class, Integer.class);
		boxMap.put(float.class, Float.class);
		boxMap.put(long.class, Long.class);
		boxMap.put(double.class, Double.class);
	}

	// public final static boolean isDynamic(Method m) {
	// return m.getAnnotation(ODynamic.class) != null;
	// }
	//
	// public final static boolean isPure(Method m, boolean isPure) {
	// if (m.getAnnotation(IPure.class) != null) {
	// return true;
	// }
	// return isPure;
	// }

	public final static boolean isDeprecated(Method m) {
		return m.getAnnotation(Deprecated.class) != null;
	}

	// public final static boolean isConst(Method m) {
	// Annotation a = m.getAnnotation(nez.iroha.api.IPure.class);
	// return a != null;
	// }

	public static boolean isFinal(Field f) {
		return Modifier.isFinal(f.getModifiers());
	}

	public final static boolean isPublicStatic(Field m) {
		int mod = m.getModifiers();
		return Modifier.isStatic(mod) && Modifier.isPublic(mod);
	}

	public final static boolean isStatic(Field m) {
		return Modifier.isStatic(m.getModifiers());
	}

	strictfp public final static boolean isPublic(Field m) {
		return Modifier.isPublic(m.getModifiers());
	}

	public final static boolean isInterface(Method m) {
		return Modifier.isInterface(m.getDeclaringClass().getModifiers());
	}

	public final static boolean isPublicStatic(Method m) {
		int mod = m.getModifiers();
		return Modifier.isStatic(mod) && Modifier.isPublic(mod);
	}

	public final static boolean isStatic(Method m) {
		return Modifier.isStatic(m.getModifiers());
	}

	//
	public final static boolean isPublic(Method m) {
		return Modifier.isPublic(m.getModifiers());
	}

	//
	public final static boolean isFinal(Method m) {
		return Modifier.isFinal(m.getModifiers()) || Modifier.isFinal(m.getDeclaringClass().getModifiers());
	}

	@OCast
	public final static boolean isVirtual(Method m) {
		return Modifier.isFinal(m.getModifiers()) || Modifier.isFinal(m.getDeclaringClass().getModifiers());
	}

	public final static Field loadField(Class<?> c, String name) {
		try {
			return c.getDeclaredField(name);
		} catch (NoSuchFieldException | SecurityException e) {
			ODebug.traceException(e);
			return null;
		}
	}

	public final static Object fieldValue(Field f, Object self) {
		try {
			return f.get(self);
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
			ODebug.traceException(e);
			return null;
		}
	}

	public final static Object loadFieldValue(Class<?> c, String name) {
		try {
			Field f = c.getDeclaredField(name);
			return f.get(null);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			ODebug.traceException(e);
			return null;
		}
	}

	public final static void setStaticField(Field f, Object value) {
		try {
			f.setAccessible(true);
			f.set(null, value);
			f.setAccessible(false);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			ODebug.traceException(e);
		}
	}

	public static Object valueField(Field f, Object self) {
		try {
			return f.get(self);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			ODebug.traceException(e);
		}
		return null;
	}

	public static Object newInstance(Class<?> c) {
		try {
			return c.newInstance();
		} catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
			ODebug.traceException(e);
		}
		return null;
	}

	public final static Method findMethod(Class<?> c, String name) {
		for (Method m : c.getDeclaredMethods()) {
			if (name.equals(m.getName())) {
				return m;
			}
		}
		return null;
	}

	public final static Method loadMethod(Class<?> c, String name, Class<?>... parameterTypes) {
		try {
			return c.getMethod(name, parameterTypes);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new OErrorCode(null, "undefined method: %s.%s(%s) by %s", c, name, parameterTypes, e);
		}
	}

	public static Constructor<?> loadConstructor(Class<?> c) {
		try {
			return c.getDeclaredConstructor();
		} catch (NoSuchMethodException | SecurityException e) {
			throw new OErrorCode(null, "undefined method: %s by %s", c, e);
		}
	}

	public static Constructor<?> loadConstructor(Class<?> c, OType... paramTypes) {
		//
		return null;
	}

	public static Constructor<?> loadConstructor(Class<?> c, Class<?>... parameterTypes) {
		try {
			return c.getDeclaredConstructor(parameterTypes);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new OErrorCode(null, "undefined method: %s(%s) by %s", c, parameterTypes, e);
		}
	}

	public static boolean nameMatch(String defined, String pattern) {
		if (pattern == null) {
			return true;
		}
		return defined.equals(pattern);
	}

	public static void listMatchedMethods(OTypeSystem ts, Class<?> c, String name, List<OMethodHandle> l,
			OListMatcher<OMethodHandle> f) {
		if (name.equals("<init>")) {
			Constructor<?>[] constructors = c.getConstructors();
			for (int i = 0; i < constructors.length; i++) {
				OMethodHandle mh = new OConstructor(ts, constructors[i]);
				if (f.isMatched(mh)) {
					l.add(mh);
				}
			}
		} else {
			// Field field = loadField(c, name);
			// if (field != null) {
			// if (!TypeUtils.isStatic(field)) {
			// OMethodHandle mh = new OGetter(new OField(ts, field));
			// if (f.isMatched(mh)) {
			// l.add(mh);
			// }
			// }
			// }
			Method[] list = c.getDeclaredMethods();
			for (Method m : list) {
				OMethodHandle mh = new OMethod(ts, m);
				if (mh.matchName(name) && f.isMatched(mh)) {
					l.add(mh);
				}
			}
		}
	}

}

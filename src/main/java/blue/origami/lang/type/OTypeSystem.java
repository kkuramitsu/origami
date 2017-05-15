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

package blue.origami.lang.type;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;

import blue.origami.asm.OClassLoader;
import blue.origami.ffi.Immutable;
import blue.origami.ffi.ONullable;
import blue.origami.ffi.OrigamiObject;
import blue.origami.lang.OEnv;
import blue.origami.nez.ast.SourcePosition;
import blue.origami.ocode.NullCode;
import blue.origami.ocode.OCode;
import blue.origami.ocode.ValueCode;
import blue.origami.util.OTypeUtils;

public abstract class OTypeSystem {

	private final OClassLoader classLoader;

	public OTypeSystem() {
		this.classLoader = new OClassLoader();
		this.define(OUntypedType.class, new OUntypedType(this));
		this.define(AnyType.class, new AnyType(this));
	}

	/* class loader */

	public final OClassLoader getClassLoader() {
		return this.classLoader;
	}

	/* env */

	public abstract void init(OEnv env, SourcePosition s);

	/* configuration */

	protected OType createClassType(Class<?> c) {
		return new OClassType(this, c);
	}

	// protected OType createArrayType(OType t) {
	// return new OArrayType(t);
	// }

	/* ArrayType */

	public String nameArrayType(OType componentType) {
		return componentType.getLocalName() + "[]";
	}

	public OType newArrayType(OType componentType) {
		Class<?> c = componentType.unwrapOrNull((Class<?>) null);
		if (c == null) {
			return new OArrayType(componentType);
		}
		return this.ofType(Array.newInstance(c, 0).getClass());
	}

	protected OType createNullableType(OType t) {
		return new NullableType(t);
	}

	protected OType createMutableType(OType t) {
		return new MutableType(t);
	}

	/* Type Factory */

	final HashMap<Class<?>, OType> typeMap = new HashMap<>();

	public final boolean isDefined(Class<?> c) {
		return this.typeMap.get(c) != null;
	}

	public final <T extends OType> T define(Class<?> c, T t) {
		this.typeMap.put(c, t);
		return t;
	}

	public final <T extends OType> T define(T t) {
		this.typeMap.put(t.unwrap(), t);
		return t;
	}

	public final OType ofType(Class<?> c) {
		OType t = this.typeMap.get(c);
		if (t == null && c != null) {
			if (c.isArray()) {
				t = this.createClassType(c.getComponentType());
				t = new OArrayType(t);
			} else {
				t = this.createClassType(c);
			}
			this.typeMap.put(c, t);
		}
		return t;
	}

	public final OType[] ofTypes(Class<?>... c) {
		OType[] inf = new OType[c.length];
		for (int i = 0; i < inf.length; i++) {
			inf[i] = this.ofType(c[i]);
		}
		return inf;
	}

	public final OType[] ofTypes(Parameter... p) {
		OType[] inf = new OType[p.length];
		for (int i = 0; i < inf.length; i++) {
			OType t = this.ofType(p[i].getParameterizedType());
			if (p[i].isAnnotationPresent(ONullable.class)) {
				t = this.newNullableType(t);
			}
			inf[i] = t;
		}
		return inf;
	}

	public final OType ofType(Type p) {
		if (p instanceof Class<?>) {
			return this.ofType((Class<?>) p);
		}
		if (p instanceof TypeVariable<?>) {
			return new OParamVarType(this, (TypeVariable<?>) p);
		}
		if (p instanceof ParameterizedType) {
			OType base = this.ofType(((ParameterizedType) p).getRawType());
			Type[] pp = ((ParameterizedType) p).getActualTypeArguments();
			return OParamType.of(base, this.ofTypes(pp));
		}
		if (p instanceof GenericArrayType) {
			OType t = this.ofType(((GenericArrayType) p).getGenericComponentType());
			return new OArrayType(t);
		}
		if (p instanceof WildcardType) {
			WildcardType w = (WildcardType) p;
			return new OParamWildcardType(this, w);
		}
		return this.ofType((Class<?>) p);
	}

	public final OType[] ofTypes(Type[] p) {
		OType[] t = new OType[p.length];
		for (int i = 0; i < p.length; i++) {
			t[i] = this.ofType(p[i]);
		}
		return t;
	}

	/* NullMap */

	final HashMap<Class<?>, OType> nullMap = new HashMap<>();

	public OType newNullableType(Class<?> c) {
		OType t = this.nullMap.get(c);
		if (t == null) {
			t = this.createNullableType(this.ofType(OTypeUtils.boxType(c)));
			this.nullMap.put(c, t);
		}
		return t;
	}

	public OType newNullableType(OType ty) {
		if (ty instanceof OClassType) {
			return this.newNullableType(ty.unwrap());
		}
		return this.createNullableType(ty);
	}

	/* MutableType */

	final HashMap<Class<?>, OType> mutMap = new HashMap<>();

	public final OType newMutableType(Class<?> c) {
		OType t = this.mutMap.get(c);
		if (t == null) {
			t = this.ofType(c);
			if (OrigamiObject.class.isAssignableFrom(c) && !Immutable.class.isAssignableFrom(c)) {
				t = this.createMutableType(t);
				this.mutMap.put(c, t);
			}
		}
		return t;
	}

	public final OType newMutableType(OType t) {
		Class<?> c = t.unwrapOrNull((Class<?>) null);
		if (c != null) {
			return this.newMutableType(c);
		}
		return this.createMutableType(t);
	}

	/* ValueType */

	public final OType valueType(Object value) {
		if (value == null) {
			return this.newNullableType(Object.class);
		}
		return this.ofType(OTypeUtils.unboxType(value.getClass())).valueType();
	}

	public final OCode newValueCode(Object value) {
		if (value == null) {
			return new NullCode(this.ofType(OUntypedType.class));
		}
		return new ValueCode(value, this.valueType(value));
	}

}

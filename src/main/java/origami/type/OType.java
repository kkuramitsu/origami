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

package origami.type;

import java.lang.reflect.Field;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.objectweb.asm.Type;

import origami.OEnv;
import origami.OEnv.OListMatcher;
import origami.asm.OCallSite;
import origami.code.DynamicCastCode;
import origami.code.OGetterCode;
import origami.code.OCastCode;
import origami.code.OCode;
import origami.code.OErrorCode;
import origami.code.OTypeCode;
import origami.code.OWarningCode;
import origami.ffi.OAlias;
import origami.ffi.OCast;
import origami.ffi.OrigamiObject;
import origami.lang.OConstructor.DefaultThisCode;
import origami.lang.OConv;
import origami.lang.OField;
import origami.lang.OMethodHandle;
import origami.lang.ONameEntity;
import origami.lang.OTypeName;
import origami.rule.OFmt;
import origami.util.OArrayUtils;
import origami.util.OTypeUtils;
import origami.util.StringCombinator;

public interface OType extends StringCombinator, OArrayUtils, OTypeName, ONameEntity {

	public final static OType[] emptyTypes = new OType[0];

	public static String key(OType[] param) {
		StringBuilder sb = new StringBuilder();
		for (OType t : param) {
			sb.append(t.typeDesc(1));
		}
		return sb.toString();
	}

	static HashMap<String, Integer> keyMap = new HashMap<>();

	public static int keyId(OType[] param) {
		String key = key(param);
		Integer id = keyMap.get(key);
		if (id == null) {
			id = keyMap.put(key, keyMap.size());
		}
		return id;
	}

	/* interface */

	public OTypeSystem getTypeSystem();

	public Class<?> unwrap();

	public default OType newType(java.lang.reflect.Type t) {
		return this.getTypeSystem().newType(t);
	}

	public default OType[] newTypes(java.lang.reflect.Type... t) {
		return this.getTypeSystem().newTypes(t);
	}

	public String getLocalName();

	// unwrap()

	public default Class<?> unwrapOrNull(Class<?> c) {
		return this.unwrap();
	}

	public default Class<?> unwrap(OEnv env) {
		return this.unwrap();
	}

	public default String getName() {
		return unwrap().getName();
	}

	public default Type asmType() {
		return Type.getType(unwrap());
	}

	public default String typeDesc(int levelGeneric) {
		StringBuilder sb = new StringBuilder();
		this.typeDesc(sb, levelGeneric);
		return sb.toString();
	}

	public void typeDesc(StringBuilder sb, int levelGeneric);

	public interface TypeChecker {
		OCode check(OEnv env, OType t, OCode code);
	}

	public final static TypeChecker EmptyTypeChecker = new TypeChecker() {

		@Override
		public OCode check(OEnv env, OType t, OCode code) {
			return code;
		}

	};

	public final static TypeChecker NullTypeChecker = new TypeChecker() {

		@Override
		public OCode check(OEnv env, OType t, OCode code) {
			if (code.getType().isNullable() && !t.isNullable()) {
				return new OWarningCode(code, OFmt.nullable);
			}
			return code;
		}

	};

	public default OCode accept(OEnv env, OCode node, TypeChecker ext) {
		OType t = this;
		node = node.refineType(env, t);
		OType f = node.getType();
		// ODebug.trace("%s %s %s %s", f, t, f.eq(t), t.isAssignableFrom(f));
		if (f.eq(t) || t.isAssignableFrom(f)) {
			if (f.isPrimitive() && t.is(Object.class)) {
				return node.boxCode(env);
			}
			return node;
		}
		return ext.check(env, t, newCastCode(env, node));
	}

	public default OCode newCastCode(OEnv env, OCode code) {
		OType t = this;
		OType f = code.getType();
		if (f.eq(t) || t.isAssignableFrom(f)) {
			return code;
		}
		if (t.is(void.class)) {
			return new OCastCode(t, OCast.UPCAST, code);
		}
		if (f.isUntyped() || f.isDynamic()) {
			return new DynamicCastCode(env, t, code);
		}
		if (f.isPrimitive() && t.is(Object.class)) {
			return code.boxCode(env);
		}
		OConv conv = OConv.getConv(env, f, t);
		// ODebug.trace("finding cast %s => %s conv=%s", f, t, conv);
		if (conv != null) {
			// ODebug.trace("finding cast cost=%d %s => %s",
			// conv.getMatchCost(), f, t);
			return new OCastCode(t, conv, code);
		}
		if (t.isAssignableFrom(f)) {
			// ODebug.trace("downcast %s => %s conv=%s", f, t, conv);
			return new OCastCode(t, OCast.DOWNCAST, code);
		}
		// ODebug.trace("stupid %s => %s conv=%s", f, t, conv);
		return new OCastCode(t, OCast.STUPID, code);
	}

	public default boolean isOrigami() {
		return this.isA(OrigamiObject.class);
	}

	public default boolean isPrimitive() {
		return false;
	}

	public default boolean isArray() {
		return false;
	}

	public default boolean isInterface() {
		return this.unwrap().isInterface();
	}

	public default String getFuncName() {
		return "apply";
	}

	public default String rename(String name) {
		return name;
	}

	public default boolean isImmutable() {
		return false;
	}

	public default boolean isUntyped() {
		return false;
	}

	public default boolean isDynamic() {
		return false;
	}

	public default boolean isNullable() {
		return false;
	}

	public default boolean isMutable() {
		return false;
	}

	public default boolean is(Class<?> c) {
		return this.unwrap() == c;
	}

	public default boolean isA(Class<?> c) {
		return c.isAssignableFrom(this.unwrap());
	}

	public default boolean eq(OType t) {
		if (this == t) {
			return true;
		}
		Class<?> c = t.unwrapOrNull(null);
		if (c == null) {
			return this.typeDesc(1).equals(t.typeDesc(1));
		}
		return this.is(c);
	}

	public default OType getSupertype() {
		return newType(this.unwrap().getSuperclass());
	}

	public default OType getGenericSupertype() {
		return this.getTypeSystem().newType(this.unwrap().getGenericSuperclass());
	}

	public default OType[] getInterfaces() {
		return newTypes(this.unwrap().getInterfaces());
	}

	public default OType[] getGenericInterfaces() {
		return this.getTypeSystem().newTypes(this.unwrap().getGenericInterfaces());
	}

	public default boolean isAssignableFrom(OType a) {
		Class<?> tc = this.unwrapOrNull(null);
		Class<?> ac = a.unwrapOrNull(null);
		if (tc != null && ac != null) {
			return tc.isAssignableFrom(ac);
		}
		if (this.eq(a)) {
			return true;
		}
		if (a.getInterfaces() != null) {
			for (OType s : a.getInterfaces()) {
				if (this.eq(s)) {
					// ODebug.trace("implements %s %s", this, s);
					return true;
				}
			}
		}
		OType s = a.getSupertype();
		if (s != null) {
			return this.isAssignableFrom(s);
		}
		return false;
	}

	public default boolean isInstance(Object o) {
		if (o == null) {
			return false;
		}
		return isAssignableFrom(newType(o.getClass()));
	}

	public default OType getBaseType() {
		return this;
	}

	public default OType[] getParamTypes() {
		TypeVariable<?>[] v = this.unwrap().getTypeParameters();
		if (v.length == 0) {
			return OType.emptyTypes;
		}
		OType[] p = new OType[v.length];
		for (int i = 0; i < v.length; i++) {
			p[i] = new OParamVarType(this.getTypeSystem(), v[i]);
		}
		return p;
	}

	public default OType toGenericType() {
		return OParamType.of(this, this.unwrap().getTypeParameters());
	}

	public default OType resolveVarType(OVarDomain dom) {
		return this; //
	}

	public default OType matchVarType(OType a, boolean subMatch, OVarDomain dom) {
		return this;
	}

	public default OType valueType() {
		return this;
	}

	public default Object getDefaultValue() {
		return null;
	}

	public default OField[] getDeclaredFields() {
		Class<?> c = this.unwrap();
		Field[] fields = c.getDeclaredFields();
		if (fields.length > 0) {
			OField[] f = new OField[fields.length];
			for (int i = 0; i < fields.length; i++) {
				f[i] = new OField(this.getTypeSystem(), fields[i]);
			}
			return f;
		}
		return OField.emptyFields;
	}

	public default OField getDeclaredField(String name) {
		Class<?> c = this.unwrap();
		Field[] fs = c.getDeclaredFields();
		for (Field f : fs) {
			if (f.getName().equals(name)) {
				return new OField(this.getTypeSystem(), f);
			}
			OAlias a = f.getAnnotation(OAlias.class);
			if (a != null && a.name().equals(name)) {
				return new OField(this.getTypeSystem(), f);
			}
		}
		return null;
	}

	public default void listMatchedMethods(String name, List<OMethodHandle> l, OListMatcher<OMethodHandle> f) {
		OTypeUtils.listMatchedMethods(this.getTypeSystem(), this.unwrap(), name, l, f);
	}

	public default OMethodHandle[] getConstructors() {
		ArrayList<OMethodHandle> l = new ArrayList<>(8);
		this.listMatchedMethods("<init>", l, (mh) -> true);
		if (l.size() > 0) {
			return l.toArray(new OMethodHandle[l.size()]);
		}
		return OMethodHandle.emptyMethods;
	}

	public default OMethodHandle[] getDeclaredMethods(String name) {
		ArrayList<OMethodHandle> l = new ArrayList<>(8);
		this.listMatchedMethods(name, l, (mh) -> true);
		if (l.size() > 0) {
			return l.toArray(new OMethodHandle[l.size()]);
		}
		return OMethodHandle.emptyMethods;
	}

	public default OCode newConstructorCode(OEnv env, final OCode... params) {
		ArrayList<OMethodHandle> l = new ArrayList<>(8);
		OCode[] p = append((OCode) new DefaultThisCode(this), params);
		this.listMatchedMethods("<init>", l, (mh) -> mh.isPublic() && mh.isThisParamSize(p.length));
		// ODebug.trace("l=%s %s", l, p[1]);
		return OCallSite.matchParamCode(env, null, "<init>", p, l);
	}

	public default OCode newGetterCode(OEnv env, OCode recv, String name) {
		OField f = this.getDeclaredField(name);
		if (f != null) {
			if (f.isPublic()) {
				return f.isStatic() ? new OGetterCode(f) : new OGetterCode(f, recv);
			}
		}
		throw new OErrorCode(env, OFmt.undefined_field__YY0_YY1, this, name);
	}

	public default OCode newStaticGetterCode(OEnv env, String name) {
		OField f = this.getDeclaredField(name);
		if (f != null) {
			if (f.isPublic() && f.isStatic()) {
				return new OGetterCode(f);
			}
		}
		throw new OErrorCode(env, OFmt.undefined_static_field__YY0_YY1, this, name);
	}

	// --------------------------------------------------------------------

	public default int castCost(OType t) {
		if (this.eq(t)) {
			return OCast.SAME;
		}
		if (this instanceof AnyType) { // Any, Integer
			return OCast.ANYCAST;
		}
		if (this.isAssignableFrom(t)) { // Number => Integer
			return OCast.DOWNCAST;
		}
		if (t.isAssignableFrom(this)) {
			return OCast.UPCAST;
		}
		return OCast.STUPID;
	}

	public default OType commonSuperType(OType a) {
		return this.getTypeSystem().newType(Object.class); // FIXME
	}

	public default OType boxType() {
		if (isPrimitive()) {
			return newType(OTypeUtils.boxType(this.unwrap()));
		}
		return this;
	}

	public default OType unboxType() {
		if (this instanceof OClassType) {
			Class<?> c = this.unwrap();
			Class<?> c2 = OTypeUtils.unboxType(c);
			if (c != c2) {
				return newType(c2);
			}
		}
		return this;
	}

	// OName interfaces

	@Override
	public default boolean isName(OEnv env) {
		return true;
	}

	@Override
	public default OCode nameCode(OEnv env, String name) {
		return new OTypeCode(this);
	}

	@Override
	public default OType inferTypeByName(OEnv env) {
		return this.valueType();
	}

	// TypeName interfaces

}

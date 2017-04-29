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

import blue.origami.asm.code.ArrayLengthCode;
import blue.origami.lang.OEnv;
import blue.origami.ocode.MultiCode;
import blue.origami.ocode.OCode;

public class OArrayType extends OTypeImpl {
	private final OType componentType;
	public static String ArrayLengthName = "length";

	public OArrayType(OType inner) {
		this.componentType = inner;
	}

	OArrayType(OTypeSystem ts, Class<?> c) {
		this.componentType = ts.ofType(c);
	}

	@Override
	public OTypeSystem getTypeSystem() {
		return this.componentType.getTypeSystem();
	}

	@Override
	public boolean isArray() {
		return true;
	}

	@Override
	public Object getDefaultValue() {
		Class<?> ctype = this.componentType.unwrapOrNull((Class<?>) null);
		if (ctype != null) {
			return Array.newInstance(ctype, 0);
		}
		return null;
	}

	@Override
	public Class<?> unwrap() {
		return Array.newInstance(this.componentType.unwrap(), 0).getClass();
	}

	@Override
	public Class<?> unwrapOrNull(Class<?> c) {
		return Array.newInstance(this.componentType.unwrapOrNull(c), 0).getClass();
	}

	@Override
	public Class<?> unwrap(OEnv env) {
		return Array.newInstance(this.componentType.unwrap(env), 0).getClass();
	}

	@Override
	public void typeDesc(StringBuilder sb, int levelGeneric) {
		sb.append("[");
		this.componentType.typeDesc(sb, levelGeneric);
	}

	@Override
	public boolean isUntyped() {
		return this.componentType.isUntyped();
	}

	@Override
	public String getLocalName() {
		return this.getTypeSystem().nameArrayType(this.componentType);
	}

	@Override
	public String getName() {
		return this.componentType.getName() + "[]";
	}

	@Override
	public OType getBaseType() {
		return this.newType(Object[].class);
	}

	@Override
	public OType[] getParamTypes() {
		return new OType[] { this.componentType };
	}

	@Override
	public OType matchVarType(OType a, boolean subMatch, OVarDomain dom) {
		if (!a.isArray()) {
			return this;
		}
		OType ctype = this.getParamTypes()[0].matchVarType(a.getParamTypes()[0], false, dom);
		if (ctype != null) {
			return this.getTypeSystem().newArrayType(ctype);
		}
		return ctype;
	}

	@Override
	public OType getSupertype() {
		return this.newType(this.unwrap().getSuperclass());
	}

	@Override
	public OType[] getInterfaces() {
		return OType.emptyTypes;
	}

	// Companions

	public static OType newType(OType inner) {
		if (inner instanceof OClassType) {
			Class<?> a = Array.newInstance(inner.unwrap(), 0).getClass();
			return inner.getTypeSystem().ofType(a);
		}
		return new OArrayType(inner);
	}

	@Override
	public OCode newGetterCode(OEnv env, OCode recv, String name) {
		if (name.equals(ArrayLengthName)) {
			return new MultiCode(recv, new ArrayLengthCode(env));
		}
		return super.newGetterCode(env, recv, name);
	}

}

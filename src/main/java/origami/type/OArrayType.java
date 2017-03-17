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

import java.lang.reflect.Array;

import origami.OEnv;
import origami.asm.code.ArrayLengthCode;
import origami.code.OCode;
import origami.code.OMultiCode;

public class OArrayType extends OTypeImpl {
	private final OType innerType;
	public static String ArrayLengthName = "length";

	public OArrayType(OType inner) {
		this.innerType = inner;
	}

	OArrayType(OTypeSystem ts, Class<?> c) {
		this.innerType = ts.newType(c);
	}

	@Override
	public OTypeSystem getTypeSystem() {
		return this.innerType.getTypeSystem();
	}

	@Override
	public boolean isArray() {
		return true;
	}

	@Override
	public Object getDefaultValue() {
		Class<?> ctype = innerType.unwrapOrNull((Class<?>) null);
		if (ctype != null) {
			return Array.newInstance(ctype, 0);
		}
		return null;
	}

	@Override
	public Class<?> unwrap() {
		return Array.newInstance(innerType.unwrap(), 0).getClass();
	}

	@Override
	public Class<?> unwrapOrNull(Class<?> c) {
		return Array.newInstance(innerType.unwrapOrNull(c), 0).getClass();
	}

	@Override
	public Class<?> unwrap(OEnv env) {
		return Array.newInstance(innerType.unwrap(env), 0).getClass();
	}

	@Override
	public void typeDesc(StringBuilder sb, int levelGeneric) {
		sb.append("[");
		innerType.typeDesc(sb, levelGeneric);
	}

	@Override
	public boolean isUntyped() {
		return innerType.isUntyped();
	}

	@Override
	public String getLocalName() {
		return innerType.getLocalName() + "[]";
	}

	@Override
	public String getName() {
		return innerType.getName() + "[]";
	}

	@Override
	public OType getBaseType() {
		return newType(Object[].class);
	}

	@Override
	public OType[] getParamTypes() {
		return new OType[] { innerType };
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
		return newType(this.unwrap().getSuperclass());
	}

	@Override
	public OType[] getInterfaces() {
		return OType.emptyTypes;
	}

	// Companions

	public static OType newType(OType inner) {
		if (inner instanceof OClassType) {
			Class<?> a = Array.newInstance(inner.unwrap(), 0).getClass();
			return inner.getTypeSystem().newType(a);
		}
		return new OArrayType(inner);
	}

	@Override
	public OCode newGetterCode(OEnv env, OCode recv, String name) {
		if (name.equals(ArrayLengthName)) {
			return new OMultiCode(recv, new ArrayLengthCode(env));
		}
		return super.newGetterCode(env, recv, name);
	}

}

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

import java.lang.reflect.TypeVariable;

import origami.ODebug;
import origami.trait.OStringBuilder;

public class OParamType extends OTypeImpl {
	private final OType base;
	private final OType[] params;

	private OParamType(OType base, OType... params) {
		this.base = base.getBaseType();
		this.params = params;
	}

	@Override
	public Class<?> unwrap() {
		return base.unwrap();
	}

	@Override
	public String getLocalName() {
		return base.getLocalName();
	}

	@Override
	public OType getBaseType() {
		return this.base;
	}

	@Override
	public OTypeSystem getTypeSystem() {
		return base.getTypeSystem();
	}

	@Override
	public boolean isA(Class<?> c) {
		return this.getBaseType().isA(c);
	}

	@Override
	public OType[] getParamTypes() {
		return this.params;
	}

	@Override
	public OType toGenericType() {
		return this;
	}

	@Override
	public void typeDesc(StringBuilder sb, int levelGeneric) {
		sb.append("L");
		sb.append(this.getName().replace('.', '/'));
		if (levelGeneric > 0) {
			sb.append("<");
			for (OType t : this.params) {
				t.typeDesc(sb, levelGeneric);
			}
			sb.append(">");
		}
		sb.append(";");
	}

	@Override
	public OType resolveVarType(OVarDomain dom) {
		boolean changed = false;
		if (params.length == 1) { /* optimized case */
			OType p = params[0].resolveVarType(dom);
			if (p != params[0]) {
				return new OParamType(this.base, p);
			}
			return this;
		}
		OType[] p = new OType[params.length];
		for (int i = 0; i < params.length; i++) {
			p[i] = params[i].resolveVarType(dom);
			if (p[i] != params[i]) {
				changed = true;
			}
		}
		if (changed) {
			return new OParamType(this.base, p);
		}
		return this;
	}

	@Override
	public OType matchVarType(OType a, boolean subMatch, OVarDomain dom) {
		return dom.matchVarType(this, a, subMatch);
	}

	@Override
	public void strOut(StringBuilder sb) {
		OStringBuilder.append(sb, this.base);
		sb.append("<");
		for (int i = 0; i < params.length; i++) {
			if (i > 0) {
				sb.append(",");
			}
			OStringBuilder.append(sb, params[i]);
		}
		sb.append(">");
	}

	public static OType of(Class<?> c, OType... p) {
		return new OParamType(p[0].newType(c), p);
	}

	public static OType of(OType base, OType... p) {
		try {
			if (p.length == 0) {
				return base;
			}
			for (int i = 0; i < p.length; i++) {
				p[i] = p[i].boxType();
			}
			return new OParamType(base, p);
		} catch (StackOverflowError e) {
			ODebug.trace("base=%s %s", base, p[0]);
		}
		return base.toGenericType();
	}

	public static OType of(OType base, TypeVariable<?>[] typeParameters) {
		if (typeParameters.length == 0) {
			return base;
		}
		OTypeSystem ts = base.getTypeSystem();
		return new OParamType(base, ts.newTypes(typeParameters));
	}

}
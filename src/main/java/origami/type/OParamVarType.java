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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import origami.util.ODebug;

public class OParamVarType extends OTypeImpl {
	private final String name;
	private final OType upperBound;
	private final boolean autoBoxing = true;

	OParamVarType(OTypeSystem ts, TypeVariable<?> v) {
		this.name = v.getName();
		this.upperBound = bound(ts, v.getBounds());
	}

	static OType bound(OTypeSystem ts, Type[] bounds) {
		if (bounds.length > 0) {
			Type pp = bounds[0];
			if (pp instanceof ParameterizedType) {
				OType base = ts.newType(((ParameterizedType) pp).getRawType());
				return base;
			}

			return ts.newType(bounds[0]);
		}
		return ts.newType(Object.class);
	}

	OParamVarType(OTypeSystem ts, String name, OType upperBound) {
		this.name = name;
		this.upperBound = upperBound;
		assert (this.upperBound != null);
	}

	@Override
	public String getLocalName() {
		return name;
	}

	@Override
	public OTypeSystem getTypeSystem() {
		return this.upperBound.getTypeSystem();
	}

	public OType getUpperBoundType() {
		return this.upperBound;
	}

	@Override
	public void typeDesc(StringBuilder sb, int levelGeneric) {
		if (levelGeneric == 2) {
			sb.append("T");
			sb.append(name);
			sb.append(";");
		} else {
			upperBound.typeDesc(sb, levelGeneric);
		}
	}

	@Override
	public Class<?> unwrap() {
		return upperBound.unwrap();
	}

	@Override
	public OType resolveVarType(OVarDomain dom) {
		OType rt = dom.resolveName(name, null);
		if (rt == null) {
			return this.upperBound;
		}
		return rt;
	}

	@Override
	public OType matchVarType(OType a, boolean subMatch, OVarDomain dom) {
		OType rt = dom.resolveName(name, null);
		if (rt == null) {
			if (autoBoxing) {
				a = a.boxType();
			}
			if (!this.upperBound.isAssignableFrom(a)) {
				ODebug.trace("Mismatched upper bounds %s %s", this, a);
				return null; // mismatched
			}
			dom.addName(name, a);
			return a;
		}
		return rt;
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("'");
		sb.append(this.name);
		// if (!this.upperBound.is(Object.class)) {
		// // sb.append(" extends ");
		// StringOut.append(sb, this.upperBound);
		// }
	}

}
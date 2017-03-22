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

package origami.lang.type;

import java.lang.reflect.WildcardType;

import origami.util.StringCombinator;

public class OParamWildcardType extends OTypeImpl {
	private final OType upperBound;

	OParamWildcardType(OTypeSystem ts, WildcardType w) {
		this.upperBound = OParamVarType.bound(ts, w.getUpperBounds());
	}

	@Override
	public String getLocalName() {
		return "?";
	}

	@Override
	public Class<?> unwrap() {
		return upperBound.unwrap();
	}

	@Override
	public OTypeSystem getTypeSystem() {
		return this.upperBound.getTypeSystem();
	}

	@Override
	public void typeDesc(StringBuilder sb, int levelGeneric) {
		if (levelGeneric == 2) {
			if (upperBound.is(Object.class)) {
				sb.append("*");
			} else {
				sb.append("+");
				upperBound.typeDesc(sb, levelGeneric);
			}
		} else {
			upperBound.typeDesc(sb, levelGeneric);
		}
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("?");
		if (!this.upperBound.is(Object.class)) {
			// sb.append(" extends ");
			StringCombinator.append(sb, this.upperBound);
		}
	}

}
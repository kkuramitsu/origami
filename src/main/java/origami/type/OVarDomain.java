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

import origami.trait.OArrayUtils;

public class OVarDomain implements OArrayUtils {
	// Type Resolver

	private VarEntry startEntry = null;

	private final static class VarEntry {
		String name;
		OType type; // upper
		VarEntry prev;

		VarEntry(String name, OType type, VarEntry prev) {
			this.name = name;
			this.type = type;
			this.prev = prev;
		}
	}

	public OType resolveName(String name, OType def) {
		VarEntry r = startEntry;
		while (r != null) {
			if (r.name.equals(name)) {
				return r.type;
			}
			r = r.prev;
		}
		return def;
	}

	public void addName(String name, OType t) {
		this.startEntry = new VarEntry(name, t, this.startEntry);
	}

	public OType matchVarType(OType p, OType a, boolean subMatch) {
		if (p.getBaseType().eq(a.getBaseType())) {
			return this.matchParamVarType(p, a, this);
		}
		// ODebug.trace("checking subtype param %s <- %s...", p, a);
		if (subMatch && p.getBaseType().isAssignableFrom(a.getBaseType())) {
			/* Dict<Integer> Dict<X> Map<String, X> Map<X, Y> */
			OType superType = a.getGenericSupertype();
			OType[] supers = append(superType, a.getGenericInterfaces());
			for (OType e : supers) {
				// ODebug.trace("checking super type %s == %s...", e, p);
				if (p.getBaseType().eq(e.getBaseType())) {
					OVarDomain dom2 = new OVarDomain();
					if (this.matchParamVarType(p, e, dom2) == null) {
						// ODebug.trace("subMatch miss %s <= %s", p, e);
						return null;
					}
					OType a2 = p.resolveVarType(dom2);
					// ODebug.trace("subMatch %s <- %s <- %s", p, a2, a);
					return matchVarType(p, a2, subMatch);
				}
				if (p.getBaseType().isAssignableFrom(e.getBaseType())) {
					OVarDomain dom2 = new OVarDomain();
					return dom2.matchVarType(p, e, subMatch);
				}
			}
			// return this.matchParamVarType(p, a, this);
		}
		return null;
	}

	private OType matchParamVarType(OType p, OType a, OVarDomain dom) {
		// ODebug.trace("paramVarMatch %s %s", p, a);
		OType[] pp = p.getParamTypes();
		OType[] pa = a.getParamTypes();
		if (pp.length != pa.length) {
			return null;
		}
		if (pp.length == 0) {
			return p;
		}
		if (pp.length == 1) { /* optimized case */
			// ODebug.trace("paramVarMatch %s %s", pp[0], pa[0]);
			OType matched = pp[0].matchVarType(pa[0], false, dom);
			if (matched == null) {
				return null;
			}
			if (matched != pp[0]) {
				return OParamType.of(p.getBaseType(), matched);
			}
			return p;
		}
		boolean changed = false;
		OType[] matched = new OType[pp.length];
		for (int i = 0; i < pp.length; i++) {
			matched[i] = pp[i].matchVarType(pa[i], false, dom);
			if (matched[i] == null) {
				return null;
			}
			if (matched[i] != pp[i]) {
				changed = true;
			}
		}
		if (changed) {
			return OParamType.of(p.getBaseType(), matched);
		}
		return p;

	}

}
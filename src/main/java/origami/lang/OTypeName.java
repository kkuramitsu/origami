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

package origami.lang;

import origami.lang.type.OType;
import origami.util.ODebug;

public interface OTypeName {

	public default boolean isTypeName(OEnv env) {
		return true;
	}

	public OType inferTypeByName(OEnv env);

	public static OTypeName newEntry(OType t) {
		return new OTypeNameImpl(t);
	}

	static class OTypeNameImpl implements OTypeName {
		OType defined;

		public OTypeNameImpl(OType t) {
			this.defined = t;
		}

		@Override
		public OType inferTypeByName(OEnv env) {
			return defined;
		}

	}

	public static OType getType(OEnv env, String name) {
		OTypeName n = env.get(name, OTypeName.class, (d, c) -> d.isTypeName(env) ? d : null);
		if (n != null) {
			return n.inferTypeByName(env);
		}
		return null;
	}

	static OType lookupSubNames(OEnv env, String name) {
		OType t = getType(env, name);
		if (t != null) {
			return getType(env, name);
		}
		for (int loc = 1; loc < name.length() - 2; loc++) {
			String subname = name.substring(loc);
			t = getType(env, subname);
			if (t != null) {
				OType p = getType(env, name.substring(0, loc));
				return mergeType(p, t);
			}
		}
		return null;
	}

	static OType mergeType(OType p, OType t) {
		ODebug.trace("merge %s %s", p, t);
		if (p == null) {
			return t;
		}
		// return t.mergeType(p);
		return t;
	}

	public static OType lookupTypeName(OEnv env, String name) {
		int loc = name.length() - 1;
		for (; loc > 0; loc--) {
			char c = name.charAt(loc);
			if (c != '\'' && !Character.isDigit(c) && c != '_') {
				break;
			}
		}
		return lookupSubNames(env, name.substring(0, loc + 1));
	}

}

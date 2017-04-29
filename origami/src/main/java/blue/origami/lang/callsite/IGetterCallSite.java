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

package blue.origami.lang.callsite;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

import blue.origami.lang.OEnv;
import blue.origami.lang.OField;
import blue.origami.lang.OGetter;
import blue.origami.lang.OMethodHandle;
import blue.origami.lang.OEnv.OListMatcher;
import blue.origami.lang.type.OType;

public class IGetterCallSite extends OGetterCallSite {
	public IGetterCallSite() {
		super(null, null, null, null);
	}

	private IGetterCallSite(OEnv env, String name, String sig, MethodType methodType) {
		super(env, name, sig, methodType);
	}

	public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, Class<?> entry,
			String sig) throws Throwable {
		return new IGetterCallSite(loadEnv(entry), name, sig, type);
	}

	@Override
	public void listMatchedMethods(OEnv env, OType base, String name, List<OMethodHandle> l,
			OListMatcher<OMethodHandle> mat) {
		int found = l.size();
		if (base == null) {
			return;
		}
		for (OType c = base; !c.is(Object.class); c = c.getSupertype()) {
			c.listMatchedMethods(name, l, mat);
			if (l.size() > found) {
				return;
			}
			OField f = c.getDeclaredField(name);
			if (f != null) {
				OMethodHandle mh = new OGetter(f);
				if (mat.isMatched(mh)) {
					l.add(mh);
					return;
				}
			}
		}
	}
}
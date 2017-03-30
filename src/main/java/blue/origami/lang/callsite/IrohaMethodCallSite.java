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

import blue.origami.code.OCode;
import blue.origami.code.OMethodCode;
import blue.origami.code.OWarningCode;
import blue.origami.lang.OEnv;
import blue.origami.lang.OMethodHandle;
import blue.origami.lang.OEnv.OListMatcher;
import blue.origami.lang.type.OType;
import blue.origami.rule.OFmt;

public class IrohaMethodCallSite extends OMethodCallSite {
	public IrohaMethodCallSite() {
		super(null, null, null, null);
	}

	private IrohaMethodCallSite(OEnv env, String name, String sig, MethodType methodType) {
		super(env, name, sig, methodType);
	}

	public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, Class<?> entry,
			String sig) throws Throwable {
		return new IrohaMethodCallSite(loadEnv(entry), name, sig, type);
	}

	@Override
	public void listMatchedMethods(OEnv env, OType base, String name, List<OMethodHandle> l,
			OListMatcher<OMethodHandle> mat) {
		env.findList(name, OMethodHandle.class, l, mat);
		if (base != null) {
			for (OType c = base; !c.is(Object.class); c = c.getSupertype()) {
				c.listMatchedMethods(name, l, mat);
			}
		}
	}

	@Override
	protected OCode staticCheck(OEnv env, OCode matched) {
		// ODebug.trace("matched=%s", matched);
		if (matched instanceof OMethodCode && matched.getParams().length > 0) {
			OMethodHandle mh = ((OMethodCode) matched).getMethod();
			if (mh != null && mh.isMutable()) {
				OCode callee = ((OMethodCode) matched).getFirst();
				if (!callee.getType().isMutable()) {
					return new OWarningCode(callee, OFmt.implicit_mutation);
				}
			}
		}
		return matched;
	}

}
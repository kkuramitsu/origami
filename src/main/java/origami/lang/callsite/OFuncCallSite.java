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

package origami.lang.callsite;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

import origami.asm.OCallSite;
import origami.lang.OEnv;
import origami.lang.OMethodHandle;
import origami.lang.OEnv.OListMatcher;
import origami.lang.type.OType;

public class OFuncCallSite extends OCallSite {

	public OFuncCallSite() {
		super(null, null, null, null);
	}

	private OFuncCallSite(OEnv env, String name, String sig, MethodType methodType) {
		super(env, name, sig, methodType);
	}

	public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, Class<?> entry,
			String sig) throws Throwable {
		return new OFuncCallSite(loadEnv(entry), name, sig, type);
	}

	@Override
	public void listMatchedMethods(OEnv env, OType base, String name, List<OMethodHandle> l,
			OListMatcher<OMethodHandle> mat) {
		env.findList(name, OMethodHandle.class, l, mat);
	}

}

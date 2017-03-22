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

import java.lang.reflect.Method;

import origami.OEnv;
import origami.asm.OCallSite;
import origami.code.OCode;
import origami.util.OArrayUtils;

public class OPartialFunc extends OMethodWrapper implements OMethodHandle, OArrayUtils {

	public final int argn;
	public final OCode argv;

	public OPartialFunc(OMethodHandle mh, int argn, OCode argv) {
		super(mh);
		this.argn = argn;
		this.argv = argv;
	}

	public OPartialFunc(OEnv env, Method m, int argn, Object o) {
		this(new OMethod(env, m), argn, env.v(o));
	}

	@Override
	public OCode matchParamCode(OEnv env, OCallSite site, OCode... params) {
		params = insert(params, this.argn, this.argv);
		return mh.matchParamCode(env, site, params);
	}

	/**
	 * FIXME: Adding all methods for OMethodHandle
	 */
}

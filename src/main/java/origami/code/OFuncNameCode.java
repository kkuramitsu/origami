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

package origami.code;

import origami.lang.OEnv;
import origami.lang.OMethodHandle;
import origami.lang.callsite.OFuncCallSite;
import origami.lang.type.OFuncType;

public class OFuncNameCode extends OParamCode<String> {
	OEnv env;
	final OMethodHandle mh;

	public OFuncNameCode(OEnv env, String name, OMethodHandle mh) {
		super(name, env.t(void.class));
		this.env = env;
		this.mh = mh;
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		return OFuncType.newFuncCode(this.env, this.mh).eval(env);
	}

	@Override
	public void generate(OGenerator gen) {
		OFuncType.newFuncCode(this.env, this.mh).generate(gen);
	}

	@Override
	public OCode newApplyCode(OEnv env, OCode... params) {
		String name = this.getHandled();
		return env.get(OFuncCallSite.class).findParamCode(env, name, params);
	}

}
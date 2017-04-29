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

package blue.origami.ocode;

import blue.origami.lang.OEnv;
import blue.origami.lang.type.OType;

public abstract class SugarCode extends OParamCode<OEnv> {

	protected SugarCode(OEnv env, OType ret) {
		super(env, ret);
	}

	protected SugarCode(OEnv env, OType ret, OCode... params) {
		super(env, ret, params);
	}

	public OEnv env() {
		return this.getHandled();
	}

	public abstract OCode desugar();

	@Override
	public Object eval(OEnv env) throws Throwable {
		return this.desugar().eval(env);
	}

	@Override
	public final void generate(OGenerator gen) {
		gen.pushSugar(this);
	}

}

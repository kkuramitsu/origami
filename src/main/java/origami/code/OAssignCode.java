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

import origami.OrigamiContext.LocalVariables;
import origami.lang.OEnv;
import origami.type.OType;

public class OAssignCode extends OParamCode<String> {
	public final boolean defined;
	// public OType type;

	public OAssignCode(OType ret, boolean defined, String name, OCode right) {
		super(name, ret, right);
		this.defined = defined;
	}

	public OAssignCode(boolean defined, String name, OCode right) {
		super(name, right.getType(), right);
		this.defined = defined;
	}

	@Override
	public boolean isDefined() {
		return this.defined;
	}

	public String getName() {
		return this.getHandled();
	}

	public OType getDefinedType() {
		return this.rightCode().getType();
	}

	public OCode rightCode() {
		return this.getParams()[0];
	}

	@Override
	public OCode refineType(OEnv env, OType req) {
		if (req.is(void.class)) {
			this.setType(req);
		}
		return this;
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		Object v = this.rightCode().eval(env);
		LocalVariables vars = env.get(LocalVariables.class);
		vars.put(this.getName(), v);
		return v;
	}

	@Override
	public void generate(OGenerator gen) {
		gen.pushAssign(this);
	}

}
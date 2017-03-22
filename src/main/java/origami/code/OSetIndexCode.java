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

import java.lang.reflect.Array;

import origami.lang.OEnv;
import origami.lang.OMethodHandle;
import origami.type.OType;

public class OSetIndexCode extends OMethodCode {
	public OSetIndexCode(OType ret, OMethodHandle m, int matchCost, OCode... nodes) {
		super(m, ret, nodes, matchCost);
	}

	@Override
	public void generate(OGenerator gen) {
		gen.pushSetIndex(this);
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		if (this.getMethod() != null) {
			super.eval(env);
		}
		Object[] values = evalParams(env, this.getParams());
		Array.set(values[0], (Integer) values[1], values[2]);
		return values[2];
	}

}

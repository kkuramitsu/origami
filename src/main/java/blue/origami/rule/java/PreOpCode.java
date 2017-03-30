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

package blue.origami.rule.java;

import blue.origami.asm.code.DupCode;
import blue.origami.code.OCode;
import blue.origami.code.OGenerator;
import blue.origami.code.OMultiCode;
import blue.origami.code.OParamCode;
import blue.origami.lang.OEnv;
import blue.origami.lang.type.OType;

public class PreOpCode extends OParamCode<String> {
	OCode setter;

	public PreOpCode(String handled, OType returnType, OCode left, OCode expr, OEnv env) {
		super(handled, returnType);
		OCode op = new OMultiCode(left.newBinaryCode(env, handled, expr), new DupCode(left));
		this.setter = left.newAssignCode(env, op);
	}

	@Override
	public void generate(OGenerator gen) {
		this.setter.generate(gen);
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		return this.setter.eval(env);
	}

	public OCode setter() {
		return this.setter;
	}
}

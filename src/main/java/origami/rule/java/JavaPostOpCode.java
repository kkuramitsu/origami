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

package origami.rule.java;

import origami.OEnv;
import origami.code.OCode;
import origami.code.OGenerator;
import origami.code.OParamCode;
import origami.type.OType;

public class JavaPostOpCode extends OParamCode<String> {
	public JavaPostOpCode(String handled, OType returnType, OCode... nodes) {
		super(handled, returnType, nodes);
	}

	@Override
	public void generate(OGenerator gen) {
		this.expr().generate(gen);
		this.setter().generate(gen);
		// gen.mBuilder.pop();
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		Object v = this.expr().eval(env);
		this.setter().eval(env);
		return v;
	}

	public OCode expr() {
		return this.nodes[0];
	}

	public OCode setter() {
		return this.nodes[1];
	}
}

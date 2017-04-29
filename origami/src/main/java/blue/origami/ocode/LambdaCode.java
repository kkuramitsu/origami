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
import blue.origami.lang.OMethodDecl;

public class LambdaCode extends OParamCode<OEnv> {

	LambdaCode(OEnv env, OMethodDecl mdecl) {
		super(env, null); // FIXME
	}

	public String[] getParamNames() {
		return null;
	}

	@Override
	public void generate(OGenerator gen) {
		gen.pushLambda(this);
	}

}

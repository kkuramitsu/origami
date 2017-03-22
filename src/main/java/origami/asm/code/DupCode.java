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

package origami.asm.code;

import origami.code.OCode;
import origami.lang.OEnv;
import origami.type.OType;

public class DupCode extends OAsmCode<OCode> {

	public DupCode(OCode code) {
		super(code, null);
	}

	@Override
	public OType getType() {
		return this.getHandled().getType();
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		return this.getHandled().eval(env);
	}

}

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

import origami.OEnv;
import origami.asm.OAsm;
import origami.code.OParamCode;
import origami.type.OType;

public class LoadArgCode extends OParamCode<Integer> implements AsmCode {

	public LoadArgCode(int n, OType returnType) {
		super(n, returnType);
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		return null;
	}

	@Override
	public void generate(OAsm gen) {
		gen.pushAsmCode(this);
	}

}

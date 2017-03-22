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

import origami.OEnv;
import origami.asm.OAsm;
import origami.code.OTryCode.CatchCode;
import origami.type.OType;

public class OTryCode extends OParamCode<CatchCode[]> {

	public OTryCode(OEnv env, OCode tryCode, CatchCode[] catchCodes, OCode finallyCode) {
		super(catchCodes, tryCode.getType(), tryCode, finallyCode);
	}

	public OCode tryClasuse() {
		return nodes[0];
	}

	public CatchCode[] catchClauses() {
		return this.getHandled();
	}

	public OCode finallyClause() {
		return nodes[1];
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		try {
			Object val = nodes[0].eval(env);
			this.finallyClause().eval(env);
			return val;
		} catch (Throwable e) {
			for (CatchCode catchCode : this.catchClauses()) {
				if (catchCode.getType().isInstance(e)) {
					this.finallyClause().eval(env);
					return catchCode.catchClause().eval(env);
				}
			}
			this.finallyClause().eval(env);
			throw e;
		}
	}

	@Override
	public void generate(OGenerator gen) {
		gen.pushTry(this);
	}

	public static class CatchCode extends OLocalCode<String> {

		public CatchCode(OType type, String name, OCode clause) {
			super(name, type, clause);
		}

		public String getName() {
			return this.getHandled();
		}

		public OCode catchClause() {
			return nodes[0];
		}

	}

}

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

import origami.code.OCode;
import origami.code.OSugarCode;
import origami.lang.OEnv;

public class JavaSwitchCode extends OSugarCode {

	public JavaSwitchCode(OEnv env, OCode... nodes) {
		super(env, env.t(void.class), nodes);
	}

	// @Override
	// public void generate(OGenerator gen) {
	// gen.pushSwitch(this);
	// }

	public OCode condition() {
		return this.nodes[0];
	}

	public OCode[] caseCode() {
		if (this.nodes[1] != null) {
			return this.nodes[1].getParams();
		}
		return null;
	}

	/**
	 * <pre>
	 * Params 0 : Condition (OCode) 1 : Case Clause(MultiCode)
	 **/
	public static class CaseCode extends OSugarCode {

		public Object value;

		public CaseCode(OEnv env, Object value, OCode... nodes) {
			super(env, env.t(void.class), nodes);
			this.value = value;
		}

		public OCode condition() {
			return this.nodes[0];
		}

		public OCode caseClause() {
			return this.nodes[1];
		}

		@Override
		public OCode desugar() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	@Override
	public OCode desugar() {
		// TODO Auto-generated method stub
		return null;
	}
}

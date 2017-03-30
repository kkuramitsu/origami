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

package blue.origami.code;

import blue.origami.asm.OrigamiBreakException;
import blue.origami.lang.OEnv;

public class OBreakCode extends OJumpCode<String> {

	private OEnv localEnv = null;

	public OBreakCode(OEnv env, String label, OCode expr) {
		super(env, label, expr);
		this.localEnv = env;
	}

	public OBreakCode(OEnv env, String label) {
		super(env, label);
	}

	public OBreakCode(OEnv env) {
		this(env, null);
	}

	public String getLabel() {
		return this.getHandled();
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		if (this.nodes.length == 1) {
			throw new OrigamiBreakException(this.nodes[0].eval(env));
		}
		throw new OrigamiBreakException();
	}

	// public OCode hookCode() {
	// if (this.nodes.length == 1) {
	// OBreakLabel label = this.localEnv.get(OBreakLabel.class);
	// return label.newHookCode(this.localEnv, this.nodes[0]);
	// }
	// return new OEmptyCode(this.localEnv);
	// }

	@Override
	public void generate(OGenerator gen) {
		gen.pushBreak(this);
	}

}

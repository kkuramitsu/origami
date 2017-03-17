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
import origami.asm.OrigamiBreakException;
import origami.code.OLabelBlockCode.OBreakLabel;

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
		if (nodes.length == 1) {
			throw new OrigamiBreakException(nodes[0].eval(env));
		}
		throw new OrigamiBreakException();
	}

	public OCode hookCode() {
		if (nodes.length == 1) {
			OBreakLabel label = localEnv.get(OBreakLabel.class);
			return label.newHookCode(localEnv, nodes[0]);
		}
		return new OEmptyCode(localEnv);
	}

	@Override
	public void generate(OAsm gen) {
		gen.pushBreak(this);
	}

}

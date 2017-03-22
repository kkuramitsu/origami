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
import origami.type.OType;

public class OIfCode extends OParamCode<OEnv> {

	public OIfCode(OEnv env, OCode cond, OCode then, OCode elsec) {
		super(env, null, cond, then, elsec);
	}

	@Override
	public boolean hasReturnCode() {
		if (this.nodes.length == 2) {
			return this.nodes[1].hasReturnCode() && this.nodes[2].hasReturnCode();
		}
		return false;
	}

	public OCode condCode() {
		return this.nodes[0];
	}

	public OCode thenCode() {
		return this.nodes[1];
	}

	public OCode elseCode() {
		return this.nodes[2];
	}

	@Override
	public OType getType() {
		return this.nodes[1].getType();
	}

	@Override
	public OCode refineType(OEnv env, OType t) {
		this.nodes[1] = this.nodes[1].refineType(env, t);
		this.nodes[2] = this.nodes[2].refineType(env, t).asType(env, this.nodes[1].getType());
		return this;
	}

	@Override
	public OCode retypeLocal() {
		if (!this.isUntyped() && !this.nodes[1].getType().eq(this.nodes[2].getType())) {
			if (!this.nodes[1].isUntyped()) {
				this.nodes[2] = this.nodes[2].asType(this.getHandled(), this.nodes[1].getType());
			} else {
				this.nodes[1] = this.nodes[1].asType(this.getHandled(), this.nodes[2].getType());
			}
		}
		return this;
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		if ((Boolean) this.condCode().eval(env)) {
			return this.thenCode().eval(env);
		}
		return this.elseCode().eval(env);
	}

	@Override
	public void generate(OGenerator gen) {
		gen.pushIf(this);
	}

}
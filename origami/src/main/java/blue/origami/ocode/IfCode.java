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
import blue.origami.lang.type.OType;

public class IfCode extends OParamCode<OEnv> {

	public IfCode(OEnv env, OCode condCode, OCode thenCode, OCode elseCode) {
		super(env, null/* unused */, condCode, thenCode, elseCode);
		this.retypeLocal();
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
	public boolean hasReturnCode() {
		if (this.nodes.length == 2) {
			return this.nodes[1].hasReturnCode() && this.nodes[2].hasReturnCode();
		}
		return false;
	}

	private OEnv env() {
		return this.getHandled();
	}

	/* type dependency */

	@Override
	public OType getType() {
		return this.nodes[1].getType();
	}

	@Override
	public OCode refineType(OEnv env, OType t) {
		this.nodes[1] = this.nodes[1].refineType(this.env(), t);
		this.nodes[2] = this.nodes[2].refineType(this.env(), t);
		return this;
	}

	@Override
	public OCode asType(OEnv env, OType t) {
		this.nodes[1] = this.nodes[1].asType(this.env(), t);
		this.nodes[2] = this.nodes[2].asType(this.env(), t);
		return this;
	}

	@Override
	public OCode asAssign(OEnv env, String name) {
		this.nodes[1] = this.nodes[1].asAssign(this.env(), name);
		this.nodes[2] = this.nodes[2].asAssign(this.env(), name);
		return this;
	}

	@Override
	public OCode retypeLocal() {
		OType t = this.nodes[1].getType();
		if (t.isUntyped()) {
			t = this.nodes[2].getType();
		}
		if (!t.isUntyped()) {
			this.asType(this.env(), t);
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
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
import origami.type.OType;

public class OIfCode extends OParamCode<OEnv> {

	public OIfCode(OEnv env, OCode cond, OCode then, OCode elsec) {
		super(env, null, cond, then, elsec);
	}

	@Override
	public boolean hasReturnCode() {
		if (nodes.length == 2) {
			return nodes[1].hasReturnCode() && nodes[2].hasReturnCode();
		}
		return false;
	}

	public OCode condition() {
		return nodes[0];
	}

	public OCode thenClause() {
		return nodes[1];
	}

	public OCode elseClause() {
		return nodes[2];
	}

	@Override
	public OType getType() {
		return nodes[1].getType();
	}

	@Override
	public OCode refineType(OEnv env, OType t) {
		nodes[1] = nodes[1].refineType(env, t);
		nodes[2] = nodes[2].refineType(env, t).asType(env, nodes[1].getType());
		return this;
	}

	// @Override
	// public boolean isUntyped() {
	// return nodes[1].isUntyped() || nodes[2].isUntyped();
	// }

	@Override
	public OCode retypeLocal() {
		if (!isUntyped() && !nodes[1].getType().eq(nodes[2].getType())) {
			if (!nodes[1].isUntyped()) {
				nodes[2] = nodes[2].asType(this.getHandled(), nodes[1].getType());
			} else {
				nodes[1] = nodes[1].asType(this.getHandled(), nodes[2].getType());
			}
		}
		return this;
	}

	@Override
	public void generate(OAsm gen) {
		gen.pushIf(this);
	}

}
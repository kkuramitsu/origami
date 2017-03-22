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

import origami.ffi.OCast;
import origami.lang.OConv.OConvCallSite;
import origami.lang.ODynamicMethodHandle;
import origami.lang.OEnv;
import origami.lang.OMethodHandle;
import origami.type.OType;

public class ODyCastCode extends OCastCode {
	private OEnv env;

	public ODyCastCode(OEnv env, OType t, OCode node) {
		super(t, node.isUntyped() ? OCast.UPCAST : OCast.ANYCAST, node);
		this.env = env;
	}

	@Override
	public OCode retypeLocal() {
		if (this.getFirst().isUntyped()) {
			return this.getFirst().asType(this.env, this.getType());
		}
		return this;
	}

	@Override
	public OMethodHandle getMethod() {
		return new ODynamicMethodHandle(this.env, this.env.get(OConvCallSite.class), "<conv>", this.getType(), 1);
	}
}
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

import java.lang.reflect.Constructor;

import origami.OEnv;
import origami.ffi.OCast;
import origami.lang.OConstructor;
import origami.lang.OMethodHandle;
import origami.type.OType;

public class OConstructorCode extends OMethodCode {

	public OConstructorCode(OMethodHandle handled, OType ret, OCode[] nodes, int matchCost) {
		super(handled, ret, nodes, matchCost);
	}

	public OConstructorCode(OMethodHandle handled, OCode[] nodes, int matchCost) {
		this(handled, handled.getReturnType(), nodes, matchCost);
	}

	public OConstructorCode(OMethodHandle handled, OCode... nodes) {
		this(handled, handled.getReturnType(), nodes, OCast.SAME);
	}

	public OConstructorCode(OEnv env, Constructor<?> c, OCode... nodes) {
		this(new OConstructor(env, c), null, nodes, OCast.SAME);
		this.setType(this.getMethod().getReturnType());
	}

	@Override
	public void generate(OGenerator gen) {
		gen.pushConstructor(this);
	}

	public OType getDeclaringClass() {
		return this.getHandled().getDeclaringClass();
	}

}
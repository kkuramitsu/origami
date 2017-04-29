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

import java.lang.reflect.Constructor;

import blue.origami.ffi.OCast;
import blue.origami.lang.OConstructor;
import blue.origami.lang.OEnv;
import blue.origami.lang.OMethodHandle;
import blue.origami.lang.type.OType;

public class NewCode extends ApplyCode {

	public NewCode(OMethodHandle handled, OType ret, OCode[] nodes, int matchCost) {
		super(handled, ret, nodes, matchCost);
	}

	public NewCode(OMethodHandle handled, OCode[] nodes, int matchCost) {
		this(handled, handled.getReturnType(), nodes, matchCost);
	}

	public NewCode(OMethodHandle handled, OCode... nodes) {
		this(handled, handled.getReturnType(), nodes, OCast.SAME);
	}

	public NewCode(OEnv env, Constructor<?> c, OCode... nodes) {
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
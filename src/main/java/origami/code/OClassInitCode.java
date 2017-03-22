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

public class OClassInitCode extends OMethodCode {

	public OClassInitCode(OMethodHandle method, OCode... nodes) {
		super(method, nodes, OCast.SAME);
		assert (method.isSpecial());
	}

	public OClassInitCode(OEnv env, Constructor<?> c, OCode... nodes) {
		super(new OConstructor(env, c), nodes, OCast.SAME);
	}

	@Override
	public OType getType() {
		return nodes[0].getTypeSystem().newType(void.class);
	}

}

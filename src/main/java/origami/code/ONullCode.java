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
import origami.type.OUntypedType;

public class ONullCode extends OValueCode {

	public ONullCode(OType ret) {
		super(null, ret);
	}

	public ONullCode(OEnv env) {
		this(env.t(OUntypedType.class));
	}

	@Override
	public OCode refineType(OEnv env, OType ty) {
		if (this.getType() instanceof OUntypedCode) {
			this.setType(ty);
		}
		return this;
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		return this.getType().getDefaultValue();
	}

}

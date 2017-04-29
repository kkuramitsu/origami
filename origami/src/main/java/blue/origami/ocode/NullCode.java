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
import blue.origami.lang.type.OUntypedType;

public class NullCode extends ValueCode {

	public NullCode(OType ret) {
		super(null, ret);
	}

	public NullCode(OEnv env) {
		this(env.t(OUntypedType.class));
	}

	@Override
	public OCode refineType(OEnv env, OType ty) {
		if (this.getType() instanceof UntypedCode) {
			this.setType(ty);
		}
		return this;
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		return this.getType().getDefaultValue();
	}

}

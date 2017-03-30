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

public class ValueCode extends OParamCode<Object> {
	public ValueCode(Object handled, OType ty) {
		super(handled, ty);
	}

	public Object getValue() {
		return this.getHandled();
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		Object v = this.getHandled();
		if (v instanceof OType) {
			try {
				return env.getClassLoader().loadClass(((OType) v).getName());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return v;
	}

	@Override
	public void generate(OGenerator gen) {
		gen.pushValue(this);
	}

}
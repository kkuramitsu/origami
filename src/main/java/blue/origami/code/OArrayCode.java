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

package blue.origami.code;

import java.lang.reflect.Array;

import blue.origami.lang.OEnv;
import blue.origami.lang.type.OType;
import blue.origami.lang.type.OUntypedType;
import blue.origami.rule.TypeAnalysis;

public class OArrayCode extends OParamCode<Void> implements TypeAnalysis {

	public OArrayCode(OType ret, OCode... nodes) {
		super(null, ret.getTypeSystem().newArrayType(ret), nodes);
	}

	public OArrayCode(OEnv env, OCode... nodes) {
		super(null, env.t(OUntypedType.class), nodes);
	}

	@Override
	public OCode refineType(OEnv env, OType req) {
		// ODebug.trace("refining array %s %s", this.getType(), req);
		if (this.isUntyped() && req.isArray()) {
			OType ctype = req.getParamTypes()[0];
			for (int i = 0; i < this.nodes.length; i++) {
				this.nodes[i] = this.nodes[i].asType(env, ctype);
			}
			this.setType(req);
		}
		return this;
	}

	// @Override
	// public OType getType() {
	// OType t = super.getType();
	// if (!t.isArray()) {
	// return TS.unique(Object[].class);
	// }
	// return t;
	// }

	@Override
	public Object eval(OEnv env) throws Throwable {
		Class<?> arrType = this.getType().unwrap(env);
		// ODebug.trace("array type=%s desc=%s", this.getType(),
		// this.getType().typeDesc());
		Object array = Array.newInstance(arrType.getComponentType(), this.nodes.length);
		for (int i = 0; i < this.nodes.length; i++) {
			Object value = this.nodes[i].eval(env);
			Array.set(array, i, value);
		}
		return array;
	}

	@Override
	public void generate(OGenerator gen) {
		gen.pushArray(this);
	}

}

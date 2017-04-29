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

import java.lang.reflect.Array;

import blue.origami.lang.OEnv;
import blue.origami.lang.OMethodHandle;
import blue.origami.lang.type.OType;

/**
 * <pre>
 * Params 0 : Receiver (OCode) 1 : Index (OCode)
 **/

public class GetIndexCode extends ApplyCode {
	public GetIndexCode(OType ret, OMethodHandle m, int matchCost, OCode... nodes) {
		super(m, ret, nodes, matchCost);
	}

	@Override
	public void generate(OGenerator gen) {
		gen.pushGetIndex(this);
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		if (this.getMethod() != null) {
			super.eval(env);
		}
		Object[] values = this.evalParams(env, this.nodes);
		return Array.get(values[0], (Integer) values[1]);
	}

	// @Override
	// public OCode newAssignCode(OEnv env, OType type, OCode right) {
	// OMethodHandle m = this.getMethod();
	// if (m == null) {
	// return new SetIndexCode(env.t(void.class), null, 0, nodes[0], nodes[1],
	// right);
	// }
	// String name = m.getLocalName().replace("get", "set");
	// OCode r = nodes[0].newMethodCode(env, name, nodes[1], right);
	// if (r instanceof MethodCode) {
	// ((MethodCode) r).getMethod();
	// }
	// }

}
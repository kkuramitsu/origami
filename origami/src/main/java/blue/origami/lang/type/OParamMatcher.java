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

package blue.origami.lang.type;

import blue.origami.ffi.OCast;
import blue.origami.lang.OEnv;
import blue.origami.lang.OMethodHandle;
import blue.origami.ocode.ArrayCode;
import blue.origami.ocode.OCode;
import blue.origami.ocode.DefaultValueCode;
import blue.origami.rule.TypeAnalysis;
import blue.origami.util.OArrayUtils;
import blue.origami.util.ODebug;

public class OParamMatcher extends OVarDomain implements OArrayUtils, TypeAnalysis {

	protected final OEnv env;
	private int totalMatchCost = 0;
	private OMethodHandle method;

	public OParamMatcher(OEnv env, OMethodHandle mh) {
		this.env = env;
		this.totalMatchCost = 0;
		this.method = mh;
	}

	private void updateCost(int cost2) {
		this.totalMatchCost = this.totalMatchCost + cost2;
	}

	public OCode[] transformParams(boolean isVarg, int paramSize, OCode[] params) {
		if (params.length > 0) {
			OCode[] p = new OCode[paramSize];
			if (params.length == paramSize) {
				if (isVarg && !params[paramSize - 1].getType().isArray()) {
					System.arraycopy(params, 0, p, 0, paramSize - 1);
					p[paramSize - 1] = new ArrayCode(env, params[paramSize - 1]);
				} else {
					System.arraycopy(params, 0, p, 0, paramSize);
				}
			} else {
				System.arraycopy(params, 0, p, 0, paramSize - 1);
				if (params.length > paramSize) {
					p[paramSize - 1] = new ArrayCode(env, slice(params, paramSize - 1, params.length));
				} else {
					p[paramSize - 1] = new DefaultValueCode(env);
				}
			}
			return p;
		}
		return params;
	}

	public int tryMatch(boolean isOrigami, OType[] p, OCode[] nodes) {
		for (int i = 0; i < p.length; i++) {
			if (!tryMatch(p[i], nodes, i)) {
				return OCast.STUPID;
			}
		}
		return this.totalMatchCost;
	}

	private boolean tryMatch(OType p, OCode[] nodes, int index) {
		assert (nodes[index] != null);
		OType a = nodes[index].getType();
		OType p2 = p.matchVarType(a, true, this);

		// ODebug.trace("tryMatch[%d] %s %s <- %s", index, p, p2, a);
		if (p2 == null) {
			ODebug.trace("tryMatch[%d] matchVarType miss %s %s", index, p, a);
			return false;
		}
		// TypeUtils.isPublic(a);
		nodes[index] = typeCheck(nodes[index], p2);
		// ODebug.trace("tryMatch[%d] (%s = %s) <- %s %s", index, p, p2, a,
		// nodes[index]);
		this.updateCost(nodes[index].getMatchCost());
		return true;
	}

	public OCode typeCheck(OCode node, OType t) {
		return node.asType(env, t);
	}

}

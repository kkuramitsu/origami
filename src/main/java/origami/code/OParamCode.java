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

import java.util.List;

import origami.lang.OEnv;
import origami.lang.OEnv.OListMatcher;
import origami.type.OType;
import origami.util.OConsts;

public abstract class OParamCode<T> extends OSourceCode<T> {
	protected final OCode[] nodes;

	protected OParamCode(T handled, OType ret, OCode... nodes) {
		super(handled, ret);
		this.nodes = nodes == null ? OConsts.emptyNodes : nodes;
	}

	protected OParamCode(T handled, OType returnType) {
		this(handled, returnType, OConsts.emptyNodes);
	}

	@Override
	public final OCode[] getParams() {
		return this.nodes;
	}

	public final OCode getFirst() {
		return this.nodes[0];
	}

	public final void boxParams(OEnv env) {
		for (int i = 0; i < this.nodes.length; i++) {
			this.nodes[i] = this.nodes[i].boxCode(env);
		}
	}

	@Override
	public final void find(List<OCode> l, OListMatcher<OCode> mat) {
		if (mat.isMatched(this)) {
			l.add(this);
		}
		for (int i = 0; i < this.nodes.length; i++) {
			this.nodes[i].find(l, mat);
		}
	}

	@Override
	public OCode retypeAll(int[] c) {
		for (int i = 0; i < this.nodes.length; i++) {
			this.nodes[i] = this.nodes[i].retypeAll(c);
		}
		if (this.isUntyped()) {
			OCode node = this.retypeLocal();
			if (c != null && node.isUntyped()) {
				c[0]++;
			}
			return node;
		}
		return this;
	}

	protected final Object[] evalParams(OEnv env, OCode[] nodes) throws Throwable {
		Object[] v = new Object[nodes.length];
		for (int i = 0; i < nodes.length; i++) {
			v[i] = nodes[i].eval(env);
		}
		return v;
	}

}
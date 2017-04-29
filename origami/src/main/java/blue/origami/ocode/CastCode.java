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

import blue.origami.ffi.OCast;
import blue.origami.lang.OConv;
import blue.origami.lang.OEnv;
import blue.origami.lang.OMethodHandle;
import blue.origami.lang.type.OType;
import blue.origami.rule.OFmt;
import blue.origami.util.OLog;

public class CastCode extends ApplyCode {

	public CastCode(OType t, OConv m, OCode... node) {
		super(m, t, node, m.getMatchCost());
	}

	public CastCode(OType t, int matchCost, OCode node) {
		super(null, t, new OCode[] { node }, matchCost);
	}

	public OType getFromType() {
		return this.getParams()[0].getType();
	}

	@Override
	public void setMatchCost(int cost) {
		super.setMatchCost(cost);
	}

	@Override
	public OCode refineType(OEnv env, OType t) {
		if (this.getType().isUntyped() && !t.isUntyped()) {
			OCode node = this.getFirst().asType(env, t);
			if (node instanceof CastCode) {
				((CastCode) node).setMatchCost(0);
			}
			return node;
		}
		return this;
	}

	public boolean isStupidCast() {
		return this.getMatchCost() >= OCast.STUPID && this.getHandled() == null;
	}

	public ErrorCode newErrorCode(OEnv env) {
		return new ErrorCode(env, this.getSourcePosition(), OFmt.studpid_cast__YY0_to_YY1, this.getFromType(),
				this.getType());
	}

	public boolean isDownCast() {
		return this.getMatchCost() >= OCast.DOWNCAST;
	}

	public OLog log() {
		return new OLog(this.getSourcePosition(), OLog.Warning, OFmt.implicit_conversion__YY0_to_YY1,
				this.getFromType(), this.getType());
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		if (this.isStupidCast()) {
			throw this.newErrorCode(env);
		}
		if (this.isDownCast()) {
			OLog.report(env, this.log());
		}
		OMethodHandle method = this.getHandled();
		if (method != null) {
			// ODebug.trace("cost=%d %s=>%s", this.getMatchCost(),
			// this.getFromType(), this.getType());
			return super.eval(env);
		}
		return this.getFirst().eval(env); // as is
	}

	@Override
	public void generate(OGenerator gen) {
		gen.pushCast(this);
	}

	// @Override
	// protected void strOutInner(StringBuilder sb) {
	// sb.append(StringCombinator.format(" %s=>%s %s", this.getFromType(),
	// this.getType(), this.getHandled()));
	// }
}
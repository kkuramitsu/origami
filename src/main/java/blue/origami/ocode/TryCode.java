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
import blue.origami.rule.ScriptAnalysis;

public class TryCode extends OParamCode<OEnv> {

	final CatchCode[] catchCode;

	public TryCode(OEnv env, OCode tryCode, OCode finallyCode, CatchCode... catchCodes) {
		super(env, null/* unused */, tryCode, finallyCode);
		this.catchCode = catchCodes;
		this.retypeLocal();
	}

	public OCode tryCode() {
		return this.nodes[0];
	}

	public CatchCode[] catchCode() {
		return this.catchCode;
	}

	public OCode finallyCode() {
		return this.nodes[1];
	}

	private OEnv env() {
		return this.getHandled();
	}

	@Override
	public OType getType() {
		return this.nodes[0].getType();
	}

	@Override
	public TryCode refineType(OEnv env, OType t) {
		this.nodes[0] = this.nodes[0].refineType(this.env(), t);
		for (CatchCode catchCode : this.catchCode()) {
			catchCode.refineType(this.env(), t);
		}
		return this;
	}

	@Override
	public TryCode asType(OEnv env, OType t) {
		this.nodes[0] = this.nodes[0].asType(this.env(), t);
		for (CatchCode catchCode : this.catchCode()) {
			catchCode.asType(this.env(), t);
		}
		return this;
	}

	@Override
	public TryCode asAssign(OEnv env, String name) {
		this.nodes[0] = this.nodes[0].asAssign(this.env(), name);
		for (CatchCode catchCode : this.catchCode()) {
			catchCode.asAssign(this.env(), name);
		}
		return this;
	}

	private OType findType() {
		if (!this.nodes[0].isUntyped()) {
			return this.nodes[0].getType();
		}
		for (CatchCode catchCode : this.catchCode()) {
			OType t = catchCode.bodyCode().getType();
			if (!t.isUntyped()) {
				return t;
			}
		}
		return this.nodes[0].getType(); // UntypedType
	}

	@Override
	public OCode retypeLocal() {
		OType t = this.findType();
		if (!t.isUntyped()) {
			this.asType(this.env(), t);
		}
		return this;
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		return ScriptAnalysis.eval(env, this);
	}

	@Override
	public void generate(OGenerator gen) {
		gen.pushTry(this);
	}

	public static class CatchCode extends OLocalCode<String> {
		private final OType exceptionType;

		public CatchCode(OType type, String name, OCode body) {
			super(name, null/* unused */, body);
			this.exceptionType = type;
		}

		public OType getExceptionType() {
			return this.exceptionType;
		}

		public String getName() {
			return this.getHandled();
		}

		public OCode bodyCode() {
			return this.nodes[0];
		}

		@Override
		public OType getType() {
			return this.nodes[0].getType();
		}

		@Override
		public OCode refineType(OEnv env, OType t) {
			this.nodes[0] = this.nodes[0].refineType(env, t);
			return this;
		}

		@Override
		public OCode asType(OEnv env, OType t) {
			this.nodes[0] = this.nodes[0].asType(env, t);
			return this;
		}

		@Override
		public OCode asAssign(OEnv env, String name) {
			this.nodes[0] = this.nodes[0].asAssign(env, name);
			return this;
		}

	}

}

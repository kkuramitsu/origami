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

package blue.origami.lang;

import blue.origami.lang.type.OType;
import blue.origami.ocode.OCode;
import blue.origami.ocode.ErrorCode;
import blue.origami.rule.OFmt;
import blue.origami.util.StringCombinator;

public abstract class OVariable implements ONameEntity, OTypeName, StringCombinator {
	private final boolean isReadOnly;
	private final String name;
	private final OType type;

	public OVariable(boolean isReadOnly, String name, OType type) {
		this.isReadOnly = isReadOnly;
		this.name = name;
		this.type = type;
	}

	public final boolean isReadOnly() {
		return this.isReadOnly;
	}

	public String getName() {
		return this.name;
	}

	public OType getType() {
		return this.type;
	}

	/* NameExpr */

	@Override
	public boolean isName(OEnv env) {
		return true;
	}

	@Override
	public abstract OCode nameCode(OEnv env, String name);

	public abstract OCode defineCode(OEnv env, OCode right);

	public OCode assignCode(OEnv env, OCode right) {
		if (this.isReadOnly()) {
			throw new ErrorCode(env, OFmt.read_only__YY0, this.getName());
		}
		return defineCode(env, right);
	}

	public OCode matchParamCode(OEnv env, OCode[] nodes) {
		return null;
	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		strOut(sb);
		return sb.toString();
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.name);
		sb.append(": ");
		this.type.strOut(sb);
	}

	/* OType */

	@Override
	public OType inferTypeByName(OEnv env) {
		return this.getType();
	}

}
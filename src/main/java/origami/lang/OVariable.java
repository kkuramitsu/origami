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

package origami.lang;

import origami.OEnv;
import origami.code.OCode;
import origami.code.OErrorCode;
import origami.rule.OFmt;
import origami.trait.OStringOut;
import origami.type.OType;

public abstract class OVariable implements ONameEntity, OTypeName, OStringOut {
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
			throw new OErrorCode(env, OFmt.fmt("%s", OFmt.read_only), this.getName());
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
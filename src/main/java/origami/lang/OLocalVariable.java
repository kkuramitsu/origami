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

import origami.code.OAssignCode;
import origami.code.OCode;
import origami.code.OErrorCode;
import origami.code.ONameCode;
import origami.rule.OFmt;
import origami.type.OType;

public class OLocalVariable extends OVariable {

	public OLocalVariable(boolean isReadOnly, String name, OType type) {
		super(isReadOnly, name, type);
	}

	public OLocalVariable(String name, OType type) {
		super(true, name, type);
	}

	public boolean used = false;

	public final void setUsed() {
		used = true;
	}

	public final boolean isUsed() {
		return used;
	}

	@Override
	public OCode nameCode(OEnv env, String name) {
		return new ONameCode(name, this.getType(), this.isReadOnly());
	}

	@Override
	public OCode defineCode(OEnv env, OCode right) {
		return new OAssignCode(this.getType(), true, this.getName(), right);
	}

	@Override
	public OCode assignCode(OEnv env, OCode right) {
		if (this.isReadOnly()) {
			throw new OErrorCode(env, OFmt.read_only__YY0, this.getName());
		}
		return new OAssignCode(this.getType(), false, this.getName(), right);
	}

}
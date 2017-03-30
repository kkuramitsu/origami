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
import blue.origami.ocode.AssignCode;
import blue.origami.ocode.OCode;
import blue.origami.ocode.ErrorCode;
import blue.origami.ocode.NameCode;
import blue.origami.rule.OFmt;

public class OFieldVariable extends OLocalVariable {

	public OFieldVariable(boolean isReadOnly, String name, OType type) {
		super(isReadOnly, name, type);
	}

	public OFieldVariable(String name, OType type) {
		super(true, name, type);
	}

	@Override
	public OCode nameCode(OEnv env, String name) {
		return new NameCode(name, this.getType(), this.isReadOnly());
	}

	@Override
	public OCode defineCode(OEnv env, OCode right) {
		return new AssignCode(this.getType(), true, this.getName(), right);
	}

	@Override
	public OCode assignCode(OEnv env, OCode right) {
		if (this.isReadOnly()) {
			throw new ErrorCode(env, OFmt.read_only__YY0, this.getName());
		}
		return new AssignCode(this.getType(), false, this.getName(), right);
	}

}
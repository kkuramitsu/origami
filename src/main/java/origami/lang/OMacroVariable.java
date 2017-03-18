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

public class OMacroVariable extends OVariable {

	private OCode expanded;

	public OMacroVariable(String name, OCode code) {
		super(true, name, code.getType());
		this.expanded = code;
	}

	@Override
	public OCode nameCode(OEnv env, String name) {
		return this.expanded;
	}

	@Override
	public OCode defineCode(OEnv env, OCode right) {
		throw new OErrorCode(env, OFmt.read_only__YY0, this.getName());
	}

}

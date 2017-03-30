/***********************************************************************
 * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
 *  *
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

import java.util.ArrayList;
import java.util.HashSet;

import blue.origami.lang.type.OType;

public interface OEnvTrait {
	public OEnv env();

	/* Name Analysis */

	public default void addLocalVariable(boolean readOnly, String name, OType ty) {
		OLocalVariable lvar = new OLocalVariable(readOnly, name, ty);
		env().add(name, lvar);
		env().add(OLocalVariable.class, lvar);
	}

	public default OVariable addLexicalVariable(String name, OType ty) {
		OLocalVariable lvar = new OLocalVariable(true, name, ty);
		env().add(lvar.getName(), lvar);
		env().add(OLocalVariable.class, lvar);
		return lvar;
	}

	public default OVariable[] getLocalVariables() {
		ArrayList<OVariable> list = new ArrayList<>();
		HashSet<String> found = new HashSet<>();
		env().findList(OLocalVariable.class.toString(), OVariable.class, list, (v) -> found.add(v.getName()));
		return list.toArray(new OVariable[list.size()]);
	}

}

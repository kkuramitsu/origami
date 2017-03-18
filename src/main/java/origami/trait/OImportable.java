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

package origami.trait;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

import origami.OEnv;
import origami.nez.ast.SourcePosition;

public interface OImportable {
	public default void importDefined(OEnv env, SourcePosition s, Set<String> names) {
		boolean allSymbols = names == null || names.contains("*");
		for (Field f : this.getClass().getDeclaredFields()) {
			if (!Modifier.isPublic(f.getModifiers())) {
				continue;
			}
			String name = definedName(f.getName());
			if (!allSymbols && !names.contains(name)) {
				continue;
			}
			env.add0(s, name, OTypeUtils.valueField(f, this));
		}
	}

	public default String definedName(String name) {
		int loc = name.lastIndexOf("__");
		return loc == -1 ? name : name.substring(0, loc);
	}

}
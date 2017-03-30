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

package blue.origami.rule.java;

import java.util.Set;

import blue.nez.ast.SourcePosition;
import blue.origami.ffi.OImportable;
import blue.origami.lang.OEnv;
import blue.origami.rule.ExpressionRules;
import blue.origami.rule.LiteralRules;
import blue.origami.rule.OrigamiAPIs;
import blue.origami.rule.StatementRules;
import blue.origami.rule.TypeRules;
import blue.origami.util.OScriptUtils;

public class JavaSet implements OImportable, OScriptUtils {
	@Override
	public void importDefined(OEnv env, SourcePosition s, Set<String> names) {
		addType(env, s, "boolean", boolean.class);
		addType(env, s, "byte", byte.class);
		addType(env, s, "char", char.class);
		addType(env, s, "double", double.class);
		addType(env, s, "float", float.class);
		addType(env, s, "int", int.class);
		addType(env, s, "long", long.class);
		addType(env, s, "short", short.class);
		addType(env, s, "void", void.class);

		addType(env, s, "String", String.class);
		addType(env, s, "Object", Object.class);

		importClass(env, s, LiteralRules.class, AllSubSymbols);
		importClass(env, s, TypeRules.class, AllSubSymbols);
		importClass(env, s, ExpressionRules.class, AllSubSymbols);
		importClass(env, s, StatementRules.class, AllSubSymbols);

		importClass(env, s, OrigamiAPIs.class, AllSubSymbols);
		importClass(env, s, ClassRules.class, AllSubSymbols);

	}

}

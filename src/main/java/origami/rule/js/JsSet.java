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

package origami.rule.js;

import java.util.Set;

import origami.OEnv;
import origami.ffi.JsCore;
import origami.nez.ast.SourcePosition;
import origami.rule.ClassRules;
import origami.rule.ExpressionRules;
import origami.rule.OrigamiAPIs;
import origami.rule.StatementRules;
import origami.rule.TypeRules;
import origami.trait.OImportable;
import origami.trait.OScriptUtils;

public class JsSet implements OImportable, OScriptUtils {
	@Override
	public void importDefined(OEnv env, SourcePosition s, Set<String> names) {
		// addType(env, t, "Unit", void.class);
		// addType(env, t, "Bool", boolean.class);
		// addType(env, t, "Int", long.class);
		// addType(env, t, "Float", double.class);
		// addType(env, t, "String", String.class);
		// addType(env, t, "Object", IObject.class);

		importClass(env, s, TypeRules.class, AllSubSymbols);
		importClass(env, s, ExpressionRules.class, AllSubSymbols);

		importClass(env, s, ExpressionRules.class, AllSubSymbols);
		importClass(env, s, StatementRules.class, AllSubSymbols);

		importClass(env, s, ClassRules.class, AllSubSymbols);

		importClass(env, s, JSLiteralRules.class, AllSubSymbols);
		importClass(env, s, JSExpressionRule.class, AllSubSymbols);
		importClass(env, s, JSStatementRules.class, AllSubSymbols);

		importClass(env, s, OrigamiAPIs.class, AllSubSymbols);

		importClass(env, s, JsCore.class, AllSubSymbols);
	}
}

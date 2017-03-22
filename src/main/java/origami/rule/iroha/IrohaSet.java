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

package origami.rule.iroha;

import java.util.Set;

import origami.OEnv;
import origami.lang.callsite.IrohaMethodCallSite;
import origami.lang.callsite.OMethodCallSite;
import origami.nez.ast.SourcePosition;
import origami.rule.ExpressionRules;
import origami.rule.LayerRules;
import origami.rule.LiteralRules;
import origami.rule.OrigamiAPIs;
import origami.rule.OrigamiDevelAPIs;
import origami.rule.StatementRules;
import origami.rule.TypeRules;
import origami.rule.iroha.OrigamiList.IList;
import origami.rule.iroha.OrigamiList.OList;
import origami.rule.unit.CelsiusUnit;
import origami.rule.unit.FahrenheitUnit;
import origami.rule.unit.KiloGramUnit;
import origami.rule.unit.MeterUnit;
import origami.rule.unit.OUnit;
import origami.rule.unit.SecondUnit;
import origami.rule.unit.UnitRules;
import origami.type.OUntypedType;
import origami.util.OImportable;
import origami.util.OScriptUtils;

public class IrohaSet implements OImportable, OScriptUtils {
	@Override
	public final void importDefined(OEnv env, SourcePosition s, Set<String> names) {
		addType(env, s, "void", void.class);
		addType(env, s, "bool", boolean.class);
		addType(env, s, "int", int.class);
		addType(env, s, "double", double.class);
		addType(env, s, "String", String.class);
		addType(env, s, "Object", IObject.class);
		addType(env, s, "Range", IRange.class);
		addType(env, s, "List", OList.class);
		addType(env, s, "List<int>", IList.class);

		/* unit */
		importClassMethod(env, s, OUnit.class);
		addType(env, s, "[m]", MeterUnit.class);
		addType(env, s, "[s]", SecondUnit.class);
		addType(env, s, "[kg]", KiloGramUnit.class);
		addType(env, s, "[C]", CelsiusUnit.class);
		addType(env, s, "[F]", FahrenheitUnit.class);

		addName(env, s, env.t(boolean.class), "b", "flag", "bool");
		addName(env, s, env.t(int.class), "m", "n", "i", "int", "nat");
		addName(env, s, env.t(double.class), "x", "y", "z", "float", "double");
		addName(env, s, env.t(String.class), "s", "t", "str", "text", "name");
		addName(env, s, env.t(OUntypedType.class), "X", "Y", "Z");

		env.add(OMethodCallSite.class, new IrohaMethodCallSite());

		importClass(env, s, StatementRules.class, AllSubSymbols);
		importClass(env, s, LiteralRules.class, AllSubSymbols);
		importClass(env, s, TypeRules.class, AllSubSymbols);
		importClass(env, s, ExpressionRules.class, AllSubSymbols);

		importClass(env, s, LayerRules.class, AllSubSymbols);
		importClass(env, s, MatchRules.class, AllSubSymbols);
		importClass(env, s, UnitRules.class, AllSubSymbols);
		importClass(env, s, IrohaRules.class, AllSubSymbols);

		importClass(env, s, OrigamiAPIs.class, AllSubSymbols);
		importClass(env, s, OrigamiDevelAPIs.class, AllSubSymbols);
		importClass(env, s, IrohaAPIs.class, AllSubSymbols);

	}

}

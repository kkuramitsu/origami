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

package blue.origami.rule.iroha;

import java.util.Set;

import blue.nez.ast.SourcePosition;
import blue.origami.ffi.OImportable;
import blue.origami.lang.OEnv;
import blue.origami.lang.callsite.IrohaMethodCallSite;
import blue.origami.lang.callsite.OMethodCallSite;
import blue.origami.lang.type.OUntypedType;
import blue.origami.rule.OrigamiExpressionRules;
import blue.origami.rule.OrigamiLiteralRules;
import blue.origami.rule.OrigamiOperatorAPIs;
import blue.origami.rule.ScriptAnalysis;
import blue.origami.rule.OrigamiCommonAPIs;
import blue.origami.rule.OrigamiStatementRules;
import blue.origami.rule.OrigamiTypeRules;
import blue.origami.rule.cop.LayerRules;
import blue.origami.rule.iroha.OrigamiList.IList;
import blue.origami.rule.iroha.OrigamiList.OList;
import blue.origami.rule.unit.CelsiusUnit;
import blue.origami.rule.unit.FahrenheitUnit;
import blue.origami.rule.unit.KiloGramUnit;
import blue.origami.rule.unit.MeterUnit;
import blue.origami.rule.unit.OUnit;
import blue.origami.rule.unit.SecondUnit;
import blue.origami.rule.unit.UnitRules;

public class IrohaSet implements OImportable, ScriptAnalysis {
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

		importClass(env, s, OrigamiStatementRules.class, AllSubSymbols);
		importClass(env, s, OrigamiLiteralRules.class, AllSubSymbols);
		importClass(env, s, OrigamiTypeRules.class, AllSubSymbols);
		importClass(env, s, OrigamiExpressionRules.class, AllSubSymbols);

		importClass(env, s, LayerRules.class, AllSubSymbols);
		importClass(env, s, MatchRules.class, AllSubSymbols);
		importClass(env, s, UnitRules.class, AllSubSymbols);
		importClass(env, s, IrohaRules.class, AllSubSymbols);

		importClass(env, s, OrigamiOperatorAPIs.class, AllSubSymbols);
		importClass(env, s, OrigamiCommonAPIs.class, AllSubSymbols);
		importClass(env, s, IrohaAPIs.class, AllSubSymbols);

	}

}

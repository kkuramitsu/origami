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

import blue.nez.ast.SourcePosition;
import blue.origami.lang.OEnv;
import blue.origami.lang.callsite.IrohaMethodCallSite;
import blue.origami.lang.callsite.OFuncCallSite;
import blue.origami.lang.callsite.OGetterCallSite;
import blue.origami.lang.callsite.OMethodCallSite;
import blue.origami.lang.type.OUntypedType;
import blue.origami.rule.OrigamiExpressionRules;
import blue.origami.rule.OrigamiLiteralRules;
import blue.origami.rule.OrigamiOperatorAPIs;
import blue.origami.rule.OrigamiCommonAPIs;
import blue.origami.rule.OrigamiTypeSystem;
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

public class IrohaTypeSystem extends OrigamiTypeSystem {

	public IrohaTypeSystem() {
		super();
	}

	@Override
	public void init(OEnv env, SourcePosition s) {
		this.addType(env, s, "void", void.class);
		this.addType(env, s, "bool", boolean.class);
		this.addType(env, s, "int", int.class);
		this.addType(env, s, "double", double.class);
		this.addType(env, s, "String", String.class);
		this.addType(env, s, "Object", IObject.class);
		this.addType(env, s, "Range", IRange.class);
		this.addType(env, s, "List", OList.class);
		this.addType(env, s, "List<int>", IList.class);

		/* unit */
		this.importClassMethod(env, s, OUnit.class);
		this.addType(env, s, "[m]", MeterUnit.class);
		this.addType(env, s, "[s]", SecondUnit.class);
		this.addType(env, s, "[kg]", KiloGramUnit.class);
		this.addType(env, s, "[C]", CelsiusUnit.class);
		this.addType(env, s, "[F]", FahrenheitUnit.class);

		this.addName(env, s, env.t(boolean.class), "b", "flag", "bool");
		this.addName(env, s, env.t(int.class), "m", "n", "i", "int", "nat");
		this.addName(env, s, env.t(double.class), "x", "y", "z", "float", "double");
		this.addName(env, s, env.t(String.class), "s", "t", "str", "text", "name");
		this.addName(env, s, env.t(OUntypedType.class), "X", "Y", "Z");

		env.add(OMethodCallSite.class, new IrohaMethodCallSite());
		env.add(OFuncCallSite.class, new OFuncCallSite());
		env.add(OMethodCallSite.class, new OMethodCallSite());
		env.add(OGetterCallSite.class, new OGetterCallSite());

		this.importClass(env, s, OrigamiStatementRules.class, AllSubSymbols);
		this.importClass(env, s, OrigamiLiteralRules.class, AllSubSymbols);
		this.importClass(env, s, OrigamiTypeRules.class, AllSubSymbols);
		this.importClass(env, s, OrigamiExpressionRules.class, AllSubSymbols);

		this.importClass(env, s, LayerRules.class, AllSubSymbols);
		this.importClass(env, s, MatchRules.class, AllSubSymbols);
		this.importClass(env, s, UnitRules.class, AllSubSymbols);
		this.importClass(env, s, IrohaRules.class, AllSubSymbols);

		this.importClass(env, s, OrigamiOperatorAPIs.class, AllSubSymbols);
		this.importClass(env, s, OrigamiCommonAPIs.class, AllSubSymbols);
		this.importClass(env, s, IrohaAPIs.class, AllSubSymbols);
	}

}

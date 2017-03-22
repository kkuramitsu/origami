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

import origami.lang.OEnv;
import origami.lang.callsite.IrohaMethodCallSite;
import origami.lang.callsite.OFuncCallSite;
import origami.lang.callsite.OGetterCallSite;
import origami.lang.callsite.OMethodCallSite;
import origami.lang.type.OUntypedType;
import origami.nez.ast.SourcePosition;
import origami.rule.ExpressionRules;
import origami.rule.LayerRules;
import origami.rule.LiteralRules;
import origami.rule.OrigamiAPIs;
import origami.rule.OrigamiDevelAPIs;
import origami.rule.OrigamiTypeSystem;
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

		this.importClass(env, s, StatementRules.class, AllSubSymbols);
		this.importClass(env, s, LiteralRules.class, AllSubSymbols);
		this.importClass(env, s, TypeRules.class, AllSubSymbols);
		this.importClass(env, s, ExpressionRules.class, AllSubSymbols);

		this.importClass(env, s, LayerRules.class, AllSubSymbols);
		this.importClass(env, s, MatchRules.class, AllSubSymbols);
		this.importClass(env, s, UnitRules.class, AllSubSymbols);
		this.importClass(env, s, IrohaRules.class, AllSubSymbols);

		this.importClass(env, s, OrigamiAPIs.class, AllSubSymbols);
		this.importClass(env, s, OrigamiDevelAPIs.class, AllSubSymbols);
		this.importClass(env, s, IrohaAPIs.class, AllSubSymbols);
	}

}

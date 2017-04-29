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

package blue.origami.rule;

import blue.nez.ast.SourcePosition;
import blue.origami.lang.OEnv;
import blue.origami.lang.callsite.OFuncCallSite;
import blue.origami.lang.callsite.OGetterCallSite;
import blue.origami.lang.callsite.OMethodCallSite;
import blue.origami.lang.type.OTypeSystem;
import blue.origami.rule.java.OrigamiClassRules;
import blue.origami.util.OConsole;
import blue.origami.util.OOption;
import blue.origami.util.OptionalFactory;

public class OrigamiTypeSystem extends OTypeSystem implements ScriptAnalysis, OptionalFactory<OrigamiTypeSystem> {

	public OrigamiTypeSystem() {
		super();
	}

	@Override
	public Class<?> keyClass() {
		return OrigamiTypeSystem.class;
	}

	@Override
	public OrigamiTypeSystem clone() {
		try {
			return this.getClass().newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			OConsole.exit(1, e);
			return null;
		}
	}

	@Override
	public void init(OOption options) {

	}

	@Override
	public void init(OEnv env, SourcePosition s) {
		this.addType(env, s, "void", void.class);
		this.addType(env, s, "bool", boolean.class);
		this.addType(env, s, "int", int.class);
		this.addType(env, s, "float", double.class);
		this.addType(env, s, "String", String.class);
		this.addType(env, s, "Object", Object.class);

		env.add(OFuncCallSite.class, new OFuncCallSite());
		env.add(OMethodCallSite.class, new OMethodCallSite());
		env.add(OGetterCallSite.class, new OGetterCallSite());

		this.importClass(env, s, OrigamiTypeRules.class, AllSubSymbols);
		this.importClass(env, s, OrigamiClassRules.class, AllSubSymbols);
		this.importClass(env, s, OrigamiStatementRules.class, AllSubSymbols);
		this.importClass(env, s, OrigamiExpressionRules.class, AllSubSymbols);
		this.importClass(env, s, OrigamiLiteralRules.class, AllSubSymbols);

		this.importClass(env, s, OrigamiOperatorAPIs.class, AllSubSymbols);
		this.importClass(env, s, OrigamiCommonAPIs.class, AllSubSymbols);
	}

}

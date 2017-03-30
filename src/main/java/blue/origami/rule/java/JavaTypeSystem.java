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

import blue.origami.lang.OEnv;
import blue.origami.lang.callsite.OFuncCallSite;
import blue.origami.lang.callsite.OGetterCallSite;
import blue.origami.lang.callsite.OMethodCallSite;
import blue.origami.nez.ast.SourcePosition;
import blue.origami.rule.ExpressionRules;
import blue.origami.rule.LiteralRules;
import blue.origami.rule.OrigamiAPIs;
import blue.origami.rule.OrigamiDevelAPIs;
import blue.origami.rule.OrigamiTypeSystem;
import blue.origami.rule.StatementRules;
import blue.origami.rule.TypeRules;

public class JavaTypeSystem extends OrigamiTypeSystem {

	public JavaTypeSystem() {
		super();
	}

	@Override
	public void init(OEnv env, SourcePosition s) {
		this.addType(env, s, "void", void.class);
		this.addType(env, s, "boolean", boolean.class);
		this.addType(env, s, "int", int.class);
		this.addType(env, s, "double", double.class);
		this.addType(env, s, "String", String.class);
		this.addType(env, s, "Object", Object.class);

		env.add(OFuncCallSite.class, new OFuncCallSite());
		env.add(OMethodCallSite.class, new OMethodCallSite());
		env.add(OGetterCallSite.class, new OGetterCallSite());

		this.importClass(env, s, StatementRules.class, AllSubSymbols);
		this.importClass(env, s, LiteralRules.class, AllSubSymbols);
		this.importClass(env, s, TypeRules.class, AllSubSymbols);
		this.importClass(env, s, ClassRules.class, AllSubSymbols);
		this.importClass(env, s, ExpressionRules.class, AllSubSymbols);

		this.importClass(env, s, OrigamiAPIs.class, AllSubSymbols);
		this.importClass(env, s, OrigamiDevelAPIs.class, AllSubSymbols);
		// importClass(env, s, IrohaAPIs.class, AllSubSymbols);
	}

}

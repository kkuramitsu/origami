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

package blue.origami.rule.js;

import blue.origami.asm.OAnno;
import blue.origami.ffi.OImportable;
import blue.origami.lang.OEnv;
import blue.origami.lang.OGlobalVariable;
import blue.origami.lang.OLocalVariable;
import blue.origami.lang.OVariable;
import blue.origami.lang.type.AnyType;
import blue.origami.lang.type.OType;
import blue.origami.nez.ast.Tree;
import blue.origami.ocode.OCode;
import blue.origami.ocode.DefaultValueCode;
import blue.origami.ocode.EmptyCode;
import blue.origami.rule.SyntaxAnalysis;
import blue.origami.rule.TypeAnalysis;
import blue.origami.rule.TypeRule;
import blue.origami.util.OArrayUtils;
import blue.origami.util.OTypeRule;

public class JSExpressionRule implements OImportable, SyntaxAnalysis, TypeAnalysis, OArrayUtils {
	class VarRule extends TypeRule {
		final boolean isReadOnly;

		VarRule(boolean isReadOnly) {
			this.isReadOnly = isReadOnly;
		}

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			String name = t.getText(_name, "");
			OAnno anno = parseAnno(env, "public,static", t.get(_anno, null));
			anno.setReadOnly(this.isReadOnly);
			OCode right = null;
			OType type = env.t(AnyType.class);
			if (t.has(_expr)) {
				right = typeExpr(env, t.get(_expr));
				if (t.has(_type)) {
					type = parseType(env, t.get(_type, null), type);
				}
				right = typeCheck(env, type, right);
			} else {
				right = new DefaultValueCode(type);
			}

			if (isTopLevel(env)) {
				OVariable var = new OGlobalVariable(env, anno, name, type, right);
				defineName(env, t, var);
				return new EmptyCode(env);
			}
			OVariable var = new OLocalVariable(this.isReadOnly, name, type);
			defineName(env, t, var);
			return var.defineCode(env, right);
		}

	}

	public OTypeRule VarDecl = new VarRule(false);
}

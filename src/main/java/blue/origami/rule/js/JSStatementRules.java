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
import blue.origami.lang.OMethodHandle;
import blue.origami.lang.OUntypedMethod;
import blue.origami.lang.type.AnyType;
import blue.origami.lang.type.OType;
import blue.origami.nez.ast.Tree;
import blue.origami.ocode.OCode;
import blue.origami.ocode.EmptyCode;
import blue.origami.rule.SyntaxAnalysis;
import blue.origami.rule.TypeAnalysis;
import blue.origami.rule.TypeRule;
import blue.origami.util.OArrayUtils;
import blue.origami.util.OTypeRule;

public class JSStatementRules implements OImportable, SyntaxAnalysis, TypeAnalysis, OArrayUtils {

	public OTypeRule FuncDecl = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OAnno anno = parseAnno(env, "public,static", t.get(_anno, null));

			String name = t.getStringAt(_name, null);

			String[] paramNames = parseParamNames(env, t.get(_param, null));
			OType[] paramTypes = parseParamTypes(env, paramNames, t.get(_param, null), env.t(AnyType.class));
			OType returnType = parseType(env, t.get(_type, null), env.t(AnyType.class));
			// returnType = OConfig.inferReturnType(env, name, returnType);
			OType[] exceptions = parseExceptionTypes(env, t.get(_throws, null));

			OCode body = parseFuncBody(env, t.get(_body, null));
			OMethodHandle mh = OUntypedMethod.newFunc(env, anno, returnType, name, paramNames, paramTypes, exceptions,
					body);
			defineName(env, t, mh);
			return new EmptyCode(env);
		}
	};
}

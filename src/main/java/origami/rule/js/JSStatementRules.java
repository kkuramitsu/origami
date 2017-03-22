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

import origami.OEnv;
import origami.asm.OAnno;
import origami.code.OCode;
import origami.code.OEmptyCode;
import origami.lang.OMethodHandle;
import origami.lang.OUntypedMethod;
import origami.nez.ast.Tree;
import origami.rule.AbstractTypeRule;
import origami.rule.SyntaxAnalysis;
import origami.rule.TypeAnalysis;
import origami.type.AnyType;
import origami.type.OType;
import origami.util.OArrayUtils;
import origami.util.OImportable;
import origami.util.OTypeRule;

public class JSStatementRules implements OImportable, SyntaxAnalysis, TypeAnalysis, OArrayUtils {

	public OTypeRule FuncDecl = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OAnno anno = parseAnno(env, "public,static", t.get(_anno, null));

			String name = t.getText(_name, null);

			String[] paramNames = parseParamNames(env, t.get(_param, null));
			OType[] paramTypes = parseParamTypes(env, paramNames, t.get(_param, null), env.t(AnyType.class));
			OType returnType = parseType(env, t.get(_type, null), env.t(AnyType.class));
			// returnType = OConfig.inferReturnType(env, name, returnType);
			OType[] exceptions = parseExceptionTypes(env, t.get(_throws, null));

			OCode body = parseUntypedCode(env, t.get(_body, null));
			OMethodHandle mh = OUntypedMethod.newFunc(env, anno, returnType, name, paramNames, paramTypes, exceptions, body);
			defineName(env, t, mh);
			return new OEmptyCode(env);
		}
	};
}

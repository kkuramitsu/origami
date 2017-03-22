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

package origami.rule.java;

import origami.OEnv;
import origami.asm.OAnno;
import origami.asm.OClassLoader;
import origami.code.OCode;
import origami.code.OEmptyCode;
import origami.code.OErrorCode;
import origami.code.OUntypedCode;
import origami.code.RunnableCode;
import origami.code.ThisCode;
import origami.lang.OClassDecl;
import origami.lang.OClassDeclType;
import origami.lang.OMethodHandle;
import origami.lang.OPartialFunc;
import origami.nez.ast.Symbol;
import origami.nez.ast.Tree;
import origami.rule.TypeRule;
import origami.rule.OFmt;
import origami.rule.OSymbols;
import origami.rule.SyntaxAnalysis;
import origami.rule.TypeAnalysis;
import origami.type.AnyType;
import origami.type.OArrayType;
import origami.type.OType;
import origami.util.OImportable;
import origami.util.OTypeRule;

public class ClassRules implements OImportable, OSymbols, SyntaxAnalysis, TypeAnalysis {

	public static final String DefaultSuperType = " DefaultSuperType";

	public OTypeRule ClassDecl = new TypeRule() {

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			/* annotations */
			OAnno anno = parseAnno(env, "public", t.get(_anno, null));
			String name = t.getText(_name, null);

			/* extends */
			OType superClass = env.get(DefaultSuperType, OType.class);
			if (t.has(_super)) {
				superClass = parseType(env, t.get(_super), superClass);
			}
			if (superClass == null) {
				superClass = env.t(Object.class)/* OType.Object */;
			}

			/* implements */
			OType[] interfaces = null;
			if (t.has(_impl)) {
				interfaces = new OType[t.get(_impl).size()];
				int i = 0;
				for (Tree<?> impl : t.get(_impl)) {
					OType implClass = parseType(env, impl,
							env.t(Object.class)/* OType.Object */);
					// typedClass(impl, implClass);
					interfaces[i] = implClass;
					i++;
				}
			}

			OType[] params = null;
			if (t.has(_list)) {
				Tree<?> list = t.get(_list);
				int size = list.size();
				params = new OType[size];
				for (int i = 0; i < size; i++) {
					params[i] = parseType(env, list.get(_list),
							env.t(AnyType.class)/* OType.Any */);
				}
			}

			OClassLoader cl = env.getClassLoader();
			OClassDeclType ct = cl.newType(env, anno, name, params, superClass, interfaces);
			ct.getDecl().addBody(t.get(_body, null));
			env.add(t, name, ct); // FIXME
			return new RunnableCode(env, ct.getDecl()::typeCheck);
		}

	};

	// private boolean hasConstructor(ClassDecl decl, Tree<?> body) {
	// for (Tree<?> sub : body) {
	// if (sub.is(_ConstructorDecl)) {
	// return true;
	// }
	// }
	// return false;
	// }

	public OTypeRule FieldDecl = new TypeRule() {

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OClassDecl cdecl = getClassContext(env);
			if (cdecl == null) {
				throw new OErrorCode(env, t, "not in class");
			}
			Tree<?> listNode = t.get(_list, null);
			OType type = parseType(env, t.get(_type, null), env.t(Object.class));
			OAnno anno = parseAnno(env, "public", t.get(_anno, null));

			for (Tree<?> sub : listNode) {
				String name;
				if (sub.get(_name).getTag().equals(Symbol.unique("ArrayName"))) {
					name = sub.get(_name).getText(_name, null);
					type = new OArrayType(type);
				} else {
					name = sub.getText(_name, null);
				}
				OCode expr = null;
				if (sub.has(_expr)) {
					expr = new OUntypedCode(env, sub.get(_expr));
				}
				cdecl.addField(anno, type, name, expr);
			}
			return new OEmptyCode(env);
		}

	};

	public OTypeRule MethodDecl = new TypeRule() {

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OClassDecl cdecl = getClassContext(env);
			if (cdecl == null) {
				throw new OErrorCode(env, t, "not in class");
			}
			OAnno anno = parseAnno(env, "public", t.get(_anno, null));
			String name = t.getText(_name, "");
			String[] paramNames = parseParamNames(env, t.get(_param, null));
			OType[] paramTypes = parseParamTypes(env, paramNames, t.get(_param, null), env.t(AnyType.class));
			OType ret = parseType(env, t.get(_type, null), env.t(AnyType.class));
			OType[] exceptions = parseExceptionTypes(env, t.get(_throws, null));

			OCode body = parseUntypedCode(env, t.get(_body));
			OMethodHandle m = cdecl.addMethod(anno, ret, name, paramNames, paramTypes, exceptions, body);
			if (anno.isStatic()) {
				env.add(t, name, m);
			} else {
				env.add(t, name, new OPartialFunc(m, 0, new ThisCode(cdecl.getType())));
			}
			return new OEmptyCode(env);
		}

	};

	public OTypeRule ConstructorDecl = new TypeRule() {

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OClassDecl cdecl = getClassContext(env);
			if (cdecl == null) {
				throw new OErrorCode(env, t, OFmt.YY0_is_not_here, OFmt.constructor);
			}

			OAnno anno = parseAnno(env, "public", t.get(_anno, null));
			String[] paramNames = parseParamNames(env, t.get(_param, null));
			OType[] paramTypes = parseParamTypes(env, paramNames, t.get(_param, null), getDefaultParamType(env));
			OType[] exceptions = parseExceptionTypes(env, t.get(_throws, null));
			OCode body = parseUntypedCode(env, t.get(_body));
			cdecl.addConstructorCode(anno, paramNames, paramTypes, exceptions, body);
			return new OEmptyCode(env);
		}

	};

	public OTypeRule NewExpr = new TypeRule() {

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OType type = parseType(env, t.get(_type),
					env.t(Object.class)/* OType.Object */);
			OCode[] params = typeParams(env, t);
			return type.newConstructorCode(env, params);
		}

	};

	public OTypeRule ExplicitConstructorInvocation = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			return null;
		}
	};

}
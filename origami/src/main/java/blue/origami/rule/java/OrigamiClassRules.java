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

import blue.nez.ast.Symbol;
import blue.nez.ast.Tree;
import blue.origami.asm.OAnno;
import blue.origami.asm.OClassLoader;
import blue.origami.ffi.OImportable;
import blue.origami.lang.OClassDecl;
import blue.origami.lang.OClassDeclType;
import blue.origami.lang.OEnv;
import blue.origami.lang.OMethodHandle;
import blue.origami.lang.OPartialFunc;
import blue.origami.lang.type.AnyType;
import blue.origami.lang.type.OArrayType;
import blue.origami.lang.type.OType;
import blue.origami.ocode.DeclCode;
import blue.origami.ocode.EmptyCode;
import blue.origami.ocode.ErrorCode;
import blue.origami.ocode.OCode;
import blue.origami.ocode.UntypedCode;
import blue.origami.rule.OFmt;
import blue.origami.rule.TypeRule;
import blue.origami.util.OTypeRule;

public class OrigamiClassRules implements OImportable {

	public static final String DefaultSuperType = " DefaultSuperType";

	public OTypeRule ClassDecl = new TypeRule() {

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			/* annotations */
			OAnno anno = this.parseAnno(env, "public", t.get(_anno, null));
			String name = t.getStringAt(_name, null);

			/* extends */
			OType superClass = env.get(DefaultSuperType, OType.class);
			if (t.has(_super)) {
				superClass = this.parseType(env, t.get(_super), superClass);
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
					OType implClass = this.parseType(env, impl,
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
					params[i] = this.parseType(env, list.get(_list),
							env.t(AnyType.class)/* OType.Any */);
				}
			}

			OClassLoader cl = env.getClassLoader();
			OClassDeclType ct = cl.newType(env, anno, name, params, superClass, interfaces);
			ct.getDecl().addBody(t.get(_body, null));
			env.add(t, name, ct); // FIXME
			return new DeclCode(env, ct.getDecl(), ct.getDecl()::typeCheck);
		}
	};

	public OTypeRule FieldDecl = new TypeRule() {

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OClassDecl cdecl = this.getClassContext(env);
			if (cdecl == null) {
				throw new ErrorCode(env, t, "not in class");
			}
			Tree<?> listNode = t.get(_list, null);
			OType type = this.parseType(env, t.get(_type, null), env.t(Object.class));
			OAnno anno = this.parseAnno(env, "public", t.get(_anno, null));

			for (Tree<?> sub : listNode) {
				String name;
				if (sub.get(_name).getTag().equals(Symbol.unique("ArrayName"))) {
					name = sub.get(_name).getStringAt(_name, null);
					type = new OArrayType(type);
				} else {
					name = sub.getStringAt(_name, null);
				}
				OCode expr = null;
				if (sub.has(_expr)) {
					expr = new UntypedCode(env, sub.get(_expr));
				}
				cdecl.addField(anno, type, name, expr);
			}
			return new EmptyCode(env);
		}

	};

	public OTypeRule MethodDecl = new TypeRule() {

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OClassDecl cdecl = this.getClassContext(env);
			if (cdecl == null) {
				throw new ErrorCode(env, t, "not in class");
			}
			OAnno anno = this.parseAnno(env, "public", t.get(_anno, null));
			String name = t.getStringAt(_name, "");
			String[] paramNames = this.parseParamNames(env, t.get(_param, null));
			OType[] paramTypes = this.parseParamTypes(env, paramNames, t.get(_param, null), env.t(AnyType.class));
			OType ret = this.parseType(env, t.get(_type, null), env.t(AnyType.class));
			OType[] exceptions = this.parseExceptionTypes(env, t.get(_throws, null));

			OCode body = this.parseFuncBody(env, t.get(_body));
			OMethodHandle m = cdecl.addMethod(anno, ret, name, paramNames, paramTypes, exceptions, body);
			if (anno.isStatic()) {
				env.add(t, name, m);
			} else {
				env.add(t, name, new OPartialFunc(m, 0, new JavaThisCode(cdecl.getType())));
			}
			return new EmptyCode(env);
		}

	};

	public OTypeRule ConstructorDecl = new TypeRule() {

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OClassDecl cdecl = this.getClassContext(env);
			if (cdecl == null) {
				throw new ErrorCode(env, t, OFmt.YY0_is_not_here, OFmt.constructor);
			}

			OAnno anno = this.parseAnno(env, "public", t.get(_anno, null));
			String[] paramNames = this.parseParamNames(env, t.get(_param, null));
			OType[] paramTypes = this.parseParamTypes(env, paramNames, t.get(_param, null),
					this.getDefaultParamType(env));
			OType[] exceptions = this.parseExceptionTypes(env, t.get(_throws, null));
			OCode body = this.parseFuncBody(env, t.get(_body));
			cdecl.addConstructorCode(anno, paramNames, paramTypes, exceptions, body);
			return new EmptyCode(env);
		}

	};

	public OTypeRule NewExpr = new TypeRule() {

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OType type = this.parseType(env, t.get(_type),
					env.t(Object.class)/* OType.Object */);
			OCode[] params = this.typeParams(env, t);
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
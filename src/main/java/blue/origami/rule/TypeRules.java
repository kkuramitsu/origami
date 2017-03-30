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

import blue.nez.ast.LocaleFormat;
import blue.nez.ast.Tree;
import blue.origami.ffi.OImportable;
import blue.origami.lang.OEnv;
import blue.origami.lang.OTypeName;
import blue.origami.lang.type.OType;
import blue.origami.ocode.OCode;
import blue.origami.ocode.ErrorCode;
import blue.origami.ocode.TypeValueCode;
import blue.origami.util.OTypeRule;

public class TypeRules implements OImportable, OSymbols, TypeAnalysis {

	@SuppressWarnings("serial")
	public static class TypeNotFoundException extends ErrorCode {

		public TypeNotFoundException(OEnv env, Tree<?> s, LocaleFormat format, Object... args) {
			super(env, s, format, args);
		}

	}

	public OTypeRule ClassType = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OType type = this.parseType(env, t);
			if (type == null) {
				throw new TypeNotFoundException(env, t, OFmt.undefined_type__YY0, t.toText());
			}
			return new TypeValueCode(type);
		}

		@Override
		public OType parseType(OEnv env, Tree<?> t) {
			String name = t.toText();
			if (name.indexOf('.') > 0) {
				try {
					return env.t(Class.forName(name));
				} catch (ClassNotFoundException e) {
					throw new TypeNotFoundException(env, t, OFmt.unfound_class__YY0_by_YY1, t.toText(), e);
				}
			}
			return OTypeName.getType(env, name);
		}
	};

	public OTypeRule ArrayType = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OType ty = this.parseType(env, t.get(_base));
			return new TypeValueCode(new blue.origami.lang.type.OArrayType(ty));
		}

	};

	public OTypeRule FuncType = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OType returnType = this.parseType(env, t.get(_base));
			Tree<?> params = t.get(_param);
			OType[] a = new OType[params.size()];
			for (int i = 0; i < params.size(); i++) {
				a[i] = this.parseType(env, params.get(i));
			}
			OType ty = blue.origami.lang.type.OFuncType.newType(env, returnType, a);
			return new TypeValueCode(ty);
		}
	};

	public OTypeRule CurryFuncType = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OType p = this.parseType(env, t.get(_base));
			OType ret = this.parseType(env, t.get(_param));
			OType ty = blue.origami.lang.type.OFuncType.newType(env, ret, p);
			return new TypeValueCode(ty);
		}
	};

	public OTypeRule NullableType = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OType p = this.parseType(env, t.get(_base));
			// OType ty = env.getTypeSystem().newNullableType(p);
			return new TypeValueCode(p);
		}
	};

}

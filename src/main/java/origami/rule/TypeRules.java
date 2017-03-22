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

package origami.rule;

import origami.OEnv;
import origami.code.OCode;
import origami.code.OErrorCode;
import origami.code.OTypeCode;
import origami.lang.OTypeName;
import origami.nez.ast.LocaleFormat;
import origami.nez.ast.Tree;
import origami.type.OType;
import origami.util.OImportable;
import origami.util.OTypeRule;

public class TypeRules implements OImportable, OSymbols, TypeAnalysis {

	@SuppressWarnings("serial")
	public static class TypeNotFoundException extends OErrorCode {

		public TypeNotFoundException(OEnv env, Tree<?> s, LocaleFormat format, Object... args) {
			super(env, s, format, args);
		}

	}

	public OTypeRule ClassType = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OType type = parseType(env, t);
			if (type == null) {
				throw new TypeNotFoundException(env, t, OFmt.undefined_type__YY0, t.toText());
			}
			return new OTypeCode(type);
		}

		private OType parseType(OEnv env, Tree<?> t) {
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
			OType ty = parseType(env, t.get(_base));
			return new OTypeCode(new origami.type.OArrayType(ty));
		}

	};

	public OTypeRule FuncType = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OType returnType = parseType(env, t.get(_base));
			Tree<?> params = t.get(_param);
			OType[] a = new OType[params.size()];
			for (int i = 0; i < params.size(); i++) {
				a[i] = parseType(env, params.get(i));
			}
			OType ty = origami.type.OFuncType.newType(env, returnType, a);
			return new OTypeCode(ty);
		}
	};

	public OTypeRule CurryFuncType = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OType p = parseType(env, t.get(_base));
			OType ret = parseType(env, t.get(_param));
			OType ty = origami.type.OFuncType.newType(env, ret, p);
			return new OTypeCode(ty);
		}
	};

	public OTypeRule NullableType = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OType p = parseType(env, t.get(_base));
			// OType ty = env.getTypeSystem().newNullableType(p);
			return new OTypeCode(p);
		}
	};

}

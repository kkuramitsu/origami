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

import java.util.Map;

import blue.nez.ast.Symbol;
import blue.nez.ast.Tree;
import blue.origami.asm.OAnno;
import blue.origami.lang.OClassDecl;
import blue.origami.lang.OEnv;
import blue.origami.lang.OMethodDecl;
import blue.origami.lang.OTypeName;
import blue.origami.lang.type.AnyType;
import blue.origami.lang.type.OType;
import blue.origami.lang.type.OUntypedType;
import blue.origami.ocode.ErrorCode;
import blue.origami.ocode.OCode;
import blue.origami.ocode.UntypedCode;
import blue.origami.ocode.ValueCode;
import blue.origami.util.ODebug;
import blue.origami.util.OLog;

public interface SyntaxAnalysis extends OSymbols, TypeAnalysis {

	// defining env

	public default void setDefiningEnv(OEnv env, OEnv defineEnv) {
		env.add(" export", defineEnv);
	}

	public default OEnv getDefiningEnv(OEnv env) {
		OEnv defineEnv = env.get(" export", OEnv.class);
		if (defineEnv == null) {
			return env;
		}
		return defineEnv;
	}

	// Context

	public default void setFunctionContext(OEnv env, OClassDecl cdecl) {
		env.set(OSymbols.ClassContext, OClassDecl.class, cdecl);
	}

	public default OClassDecl getClassContext(OEnv env) {
		return env.get(OSymbols.ClassContext, OClassDecl.class);
	}

	public default OClassDecl checkClassContext(OEnv env) {
		OClassDecl cdecl = env.get(OSymbols.ClassContext, OClassDecl.class);
		if (cdecl == null) {
			throw new ErrorCode(env, "not in class");
		}
		return cdecl;
	}

	public default void setFunctionContext(OEnv env, OMethodDecl mdecl) {
		env.set(OSymbols.FunctionContext, OMethodDecl.class, mdecl);
	}

	public default OMethodDecl getFunctionContext(OEnv env) {
		return env.get(OSymbols.FunctionContext, OMethodDecl.class);
	}

	public default boolean isTopLevel(OEnv env) {
		return env.get(OSymbols.FunctionContext, OMethodDecl.class) == null;
	}

	public default void defineName(OEnv env, Tree<?> t, Object d) {
		String name = t.getStringAt(_name, null);
		if (name != null) {
			env.add(t, name, d);
		}
		name = t.getStringAt(_alias, null);
		if (name != null) {
			env.add(t, name, d);
		}
	}

	// Parse

	final static Symbol _Annotation = Symbol.unique("Annotation");
	final static Symbol _Pure = Symbol.unique("Pure");
	final static Symbol _Dynamic = Symbol.unique("Dynamic");
	final static Symbol _Method = Symbol.unique("Method");

	public default OAnno A(String init) {
		return new OAnno(init);
	}

	public default OAnno parseAnno(OEnv env, String init, Tree<?> annos) {
		OAnno anno = new OAnno(init);
		if (annos != null) {
			for (Tree<?> sub : annos) {
				if (sub.is(_Annotation)) {
					String name = sub.getStringAt(_name, null);
					Map<String, Object> value = null; // FIXME
					Class<?> c = env.get("@" + name, OType.class).unwrap();
					if (c == null || !c.isAnnotation()) {
						OLog.reportWarning(env, "FIXME undefined annotation: " + name);
						continue;
					}
					anno.setAnnotation(c, value);
				} else {
					anno.add(sub.getTag().getSymbol().toLowerCase());
				}
				// else if (sub.is(_Pure)) {
				// // anno.setAnnotation(OPure.class);
				// } else if (sub.is(_Dynamic)) {
				// anno.setAnnotation(ODynamic.class);
				// } else if (sub.is(_Method)) {
				// // anno.setAnnotation(OMethod.class);
				// } else {
				// anno.acc = acc(anno.acc, sub);
				// }
			}
		}
		return anno;
	}

	final static String[] emptyNames = new String[0];

	public default String[] parseNames(OEnv env, Tree<?> names) {
		if (names == null) {
			return emptyNames;
		}
		String[] p = new String[names.size()];
		int i = 0;
		for (Tree<?> sub : names) {
			p[i] = sub.getString();
			i++;
		}
		return p;
	}

	public default String[] parseParamNames(OEnv env, Tree<?> params) {
		if (params == null) {
			return emptyNames;
		} else if (params.has(_name)) {
			return new String[] { params.getStringAt(_name, "") };
		} else {
			String[] paramNames = new String[params.size()];
			int i = 0;
			for (Tree<?> sub : params) {
				paramNames[i] = sub.getStringAt(_name, "");
				i++;
			}
			return paramNames;
		}
	}

	public default void setDefaultParamType(OEnv env, OType t) {
		env.add(OUntypedType.class, t);
	}

	public default OType getDefaultParamType(OEnv env) {
		return env.get(OUntypedType.class);
	}

	public default OType[] parseParamTypes(OEnv env, String[] paramNames, Tree<?> params, OType defaultType) {
		if (params == null) {
			return OType.emptyTypes;
		}
		OType[] p = new OType[paramNames.length];
		if (params.has(_name) && p.length == 1) {
			p[0] = parseParamType(env, params, paramNames[0], params.get(_type, null), defaultType);
			return p;
		}
		int i = 0;
		for (Tree<?> sub : params) {
			p[i] = parseParamType(env, sub, paramNames[i], sub.get(_type, null), defaultType);
			i++;
		}
		return p;
	}

	public default OType parseParamType(OEnv env, Tree<?> param, String name, Tree<?> type, OType defaultType) {
		// Symbol paramTag = param.getTag();
		OType ty = null;
		if (type != null) {
			ty = parseType(env, type, null);
		}
		if (ty == null && param.has(_useTypeHint)) {
			if (name != null) {
				ty = OTypeName.lookupTypeName(env, name);
			}
		}
		if (ty == null && param.has(_useDynamicType)) {
			if (name != null) {
				ty = env.getTypeSystem().ofType(AnyType.class);
			}
		}
		if (ty == null) {
			if (defaultType == null) {
				throw new ErrorCode(env, param, OFmt.no_typing_hint__YY0, param.getString());
			}
			ty = defaultType;
		}
		ty = parseTypeArity(env, ty, param);
		return ty;
	}

	public default OType parseTypeArity(OEnv env, OType ty, Tree<?> param) {
		if (param.has(_suffix)) {
			String suffix = param.getStringAt(_suffix, "");
			if (suffix.equals("?")) {
				ty = env.getTypeSystem().newNullableType(ty);
				ODebug.trace("arity %s", ty);
				return ty;
			}
			if (suffix.equals("+") || suffix.equals("*")) {
				ty = env.getTypeSystem().newArrayType(ty);
				ODebug.trace("arity %s", ty);
				return ty;
			}
		}
		return ty;
	}

	public default OType[] parseTypes(OEnv env, Tree<?> types) {
		if (types == null) {
			return OType.emptyTypes;
		}
		OType[] p = new OType[types.size()];
		int i = 0;
		for (Tree<?> sub : types) {
			p[i] = parseType(env, sub, env.t(OUntypedType.class));
			i++;
		}
		return p;
	}

	public default OType[] parseInterfaceTypes(OEnv env, Tree<?> types) {
		if (types == null) {
			return null;
		}
		OType[] p = new OType[types.size()];
		int i = 0;
		for (Tree<?> sub : types) {
			p[i] = parseType(env, sub, null);
			if (p[i] == null || !p[i].isInterface()) {
				throw new ErrorCode(env, sub, OFmt.YY0_is_not_interface, sub.getString());
			}
			i++;
		}
		return p;
	}

	public default OType[] parseExceptionTypes(OEnv env, Tree<?> types) {
		if (types == null) {
			return null;
		}
		OType[] p = new OType[types.size()];
		int i = 0;
		for (Tree<?> sub : types) {
			p[i] = parseType(env, sub, null);
			if (p[i] == null || !p[i].isA(Throwable.class)) {
				throw new ErrorCode(env, sub, OFmt.YY0_is_not_throwable, sub.getString());
			}
			i++;
		}
		return p;
	}

	public default OCode parseFuncBody(OEnv env, Tree<?> body) {
		return body == null ? null : new UntypedCode(env, body);
	}

	public default Object parseConstantValue(OEnv env, Tree<?> t) {
		if (t == null) {
			ODebug.trace("t=%s", t);
			return null;
		}
		OCode c = typeExpr(env, t);
		if (c instanceof ValueCode) {
			return ((ValueCode) c).getValue();
		}
		throw new ErrorCode(env, t, OFmt.YY0_is_not_constant_value, t.toString());
	}

}

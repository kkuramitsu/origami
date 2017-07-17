package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.rule.OFmt;
import blue.origami.rule.OSymbols;
import blue.origami.transpiler.TConsts;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.code.TErrorCode;

public class SyntaxRule extends LoggerRule implements OSymbols {
	// public OAnno parseAnno(TEnv env, String init, Tree<?> annos) {
	// OAnno anno = new OAnno(init);
	// if (annos != null) {
	// for (Tree<?> sub : annos) {
	// if (sub.is(_Annotation)) {
	// String name = sub.getStringAt(_name, null);
	// Map<String, Object> value = null; // FIXME
	// Class<?> c = env.get("@" + name, TType.class).unwrap();
	// if (c == null || !c.isAnnotation()) {
	// OLog.reportWarning(env, "FIXME undefined annotation: " + name);
	// continue;
	// }
	// anno.setAnnotation(c, value);
	// } else {
	// anno.add(sub.getTag().getSymbol().toLowerCase());
	// }
	// // else if (sub.is(_Pure)) {
	// // // anno.setAnnotation(OPure.class);
	// // } else if (sub.is(_Dynamic)) {
	// // anno.setAnnotation(ODynamic.class);
	// // } else if (sub.is(_Method)) {
	// // // anno.setAnnotation(OMethod.class);
	// // } else {
	// // anno.acc = acc(anno.acc, sub);
	// // }
	// }
	// }
	// return anno;
	// }

	final static String[] emptyNames = new String[0];

	public String[] parseNames(TEnv env, Tree<?> names) {
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

	public String[] parseParamNames(TEnv env, Tree<?> params) {
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

	// public void setDefaultParamType(TEnv env, TType t) {
	// env.add(OUntypedType.class, t);
	// }
	//
	// public TType getDefaultParamType(TEnv env) {
	// return env.get(OUntypedType.class);
	// }

	public TType[] parseParamTypes(TEnv env, String[] paramNames, Tree<?> params, TType defaultType) {
		if (params == null) {
			return TConsts.emptyTypes;
		}
		TType[] p = new TType[paramNames.length];
		if (params.has(_name) && p.length == 1) {
			p[0] = this.parseParamType(env, params, paramNames[0], params.get(_type, null), defaultType);
			return p;
		}
		int i = 0;
		for (Tree<?> sub : params) {
			p[i] = this.parseParamType(env, sub, paramNames[i], sub.get(_type, null), defaultType);
			i++;
		}
		return p;
	}

	public TType parseParamType(TEnv env, Tree<?> param, String name, Tree<?> type, TType defaultType) {
		// Symbol paramTag = param.getTag();
		TType ty = null;
		if (type != null) {
			ty = env.parseType(env, type, null);
		}
		if (name != null) {
			ty = env.lookupTypeHint(env, name);
		}
		if (ty == null) {
			if (defaultType == null) {
				throw new TErrorCode(param, OFmt.no_typing_hint__YY0, param.getString());
			}
			ty = defaultType;
		}
		ty = this.parseTypeArity(env, ty, param);
		return ty;
	}

	public TType parseTypeArity(TEnv env, TType ty, Tree<?> param) {
		// if (param.has(_suffix)) {
		// String suffix = param.getStringAt(_suffix, "");
		// if (suffix.equals("?")) {
		// ty = env.getTypeSystem().newNullableType(ty);
		// ODebug.trace("arity %s", ty);
		// return ty;
		// }
		// if (suffix.equals("*")) {
		// ty = env.getTypeSystem().newArrayType(ty);
		// ODebug.trace("arity %s", ty);
		// return ty;
		// }
		// }
		return ty;
	}

	public TType[] parseTypes(TEnv env, Tree<?> types) {
		if (types == null) {
			return TConsts.emptyTypes;
		}
		TType[] p = new TType[types.size()];
		int i = 0;
		for (Tree<?> sub : types) {
			p[i] = env.parseType(env, sub, null);
			i++;
		}
		return p;
	}

	// public TType[] parseInterfaceTypes(TEnv env, Tree<?> types) {
	// if (types == null) {
	// return null;
	// }
	// TType[] p = new TType[types.size()];
	// int i = 0;
	// for (Tree<?> sub : types) {
	// p[i] = parseType(env, sub, null);
	// if (p[i] == null || !p[i].isInterface()) {
	// throw new ErrorCode(env, sub, OFmt.YY0_is_not_interface,
	// sub.getString());
	// }
	// i++;
	// }
	// return p;
	// }
	//
	// public TCode parseFuncBody(TEnv env, Tree<?> body) {
	// return body == null ? null : new UntypedCode(env, body);
	// }
	//
	// public Object parseConstantValue(TEnv env, Tree<?> t) {
	// if (t == null) {
	// ODebug.trace("t=%s", t);
	// return null;
	// }
	// TCode c = typeExpr(env, t);
	// if (c instanceof ValueCode) {
	// return ((ValueCode) c).getValue();
	// }
	// throw new ErrorCode(env, t, OFmt.YY0_is_not_constant_value,
	// t.toString());
	// }

}

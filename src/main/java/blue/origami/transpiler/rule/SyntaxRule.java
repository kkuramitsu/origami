package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.type.Ty;

public class SyntaxRule extends LoggerRule implements Symbols {

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

	Ty parseReturnType(TEnv env, Tree<?> type) {
		return this.parseReturnType(env, null, type);
	}

	Ty parseReturnType(TEnv env, String name, Tree<?> type) {
		if (type != null) {
			return env.parseType(env, type, null);
		}
		if (name != null) {
			if (name.endsWith("?")) {
				return Ty.tBool;
			}
		}
		return Ty.tNULL;
	}

	Ty[] parseParamTypes(TEnv env, String[] paramNames, Tree<?> params) {
		return this.parseParamTypes(env, paramNames, params, null);
	}

	Ty[] parseParamTypes(TEnv env, String[] paramNames, Tree<?> params, Ty defaultType) {
		if (params == null) {
			return TArrays.emptyTypes;
		}
		Ty[] p = new Ty[paramNames.length];
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

	Ty parseParamType(TEnv env, Tree<?> param, String name, Tree<?> type, Ty defaultType) {
		Ty ty = null;
		if (type != null) {
			ty = env.parseType(env, type, null);
		}
		if (ty == null && name != null) {
			if (name.endsWith("?")) {
				ty = Ty.tBool;
			} else {
				NameHint hint = env.findNameHint(env, name);
				if (hint != null) {
					ty = hint.getType();
				}
			}
		}
		if (ty == null) {
			if (NameHint.isOneLetterName(name)) {
				ty = Ty.tNULL;
			}
		}
		// ty = this.parseTypeArity(env, ty, param);
		if (ty == null) {
			if (defaultType != null) {
				ty = defaultType;
			} else {
				throw new ErrorCode(param, TFmt.no_type_hint__YY1, param.getString());
			}
		}
		return ty;
	}
	//
	// // name
	// public Ty parseTypeArity(TEnv env, Ty ty, Tree<?> param) {
	// if (param.has(_suffix)) {
	// String suffix = param.getStringAt(_suffix, "");
	// // if (ty != null && suffix.equals("?")) {
	// // ty = TType.tOption(ty);
	// // ODebug.trace("arity %s", ty);
	// // return ty;
	// // }
	// if (ty != null && suffix.equals("*")) {
	// ty = Ty.tImList(ty);
	// ODebug.trace("arity %s", ty);
	// return ty;
	// }
	// }
	// return ty;
	// }

	public Ty[] parseTypes(TEnv env, Tree<?> types) {
		if (types == null) {
			return TArrays.emptyTypes;
		}
		Ty[] p = new Ty[types.size()];
		int i = 0;
		for (Tree<?> sub : types) {
			p[i] = env.parseType(env, sub, null);
			i++;
		}
		return p;
	}

}

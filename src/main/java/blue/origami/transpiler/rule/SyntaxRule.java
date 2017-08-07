package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.rule.OSymbols;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.TNameHint;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.code.TErrorCode;
import blue.origami.util.ODebug;

public class SyntaxRule extends LoggerRule implements OSymbols {

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

	public Ty[] parseParamTypes(TEnv env, String[] paramNames, Tree<?> params, Ty defaultType) {
		if (params == null) {
			return TArrays.emptyTypes;
		}
		/* We create a new env for local name hint */
		TEnv lenv = env.newEnv();
		Ty[] p = new Ty[paramNames.length];
		if (params.has(_name) && p.length == 1) {
			p[0] = this.parseParamType(lenv, params, paramNames[0], params.get(_type, null), defaultType);
			return p;
		}
		int i = 0;
		for (Tree<?> sub : params) {
			p[i] = this.parseParamType(lenv, sub, paramNames[i], sub.get(_type, null), defaultType);
			i++;
		}
		return p;
	}

	public Ty parseParamType(TEnv env, Tree<?> param, String name, Tree<?> type, Ty defaultType) {
		// Symbol paramTag = param.getTag();
		Ty ty = null;
		if (type != null) {
			ty = env.parseType(env, type, null);
		}
		if (ty == null && name != null) {
			if (name.endsWith("?")) {
				ty = Ty.tBool;
			} else {
				TNameHint hint = env.findNameHint(env, name);
				if (hint != null) {
					ty = hint.getType();
				}
			}
		}
		if (ty == null) {
			if (TNameHint.isShortName(name)) {
				ODebug.trace("local data variable %s", name);
				ty = Ty.tData().asParameter();
				env.addTypeHint(env, name, ty);
			}
		}
		ty = this.parseTypeArity(env, ty, param);
		if (ty == null) {
			if (defaultType != null) {
				ty = defaultType;
			} else {
				throw new TErrorCode(param, TFmt.no_typing_hint__YY0, param.getString());
			}
		}
		return ty;
	}

	// name
	public Ty parseTypeArity(TEnv env, Ty ty, Tree<?> param) {
		if (param.has(_suffix)) {
			String suffix = param.getStringAt(_suffix, "");
			// if (ty != null && suffix.equals("?")) {
			// ty = TType.tOption(ty);
			// ODebug.trace("arity %s", ty);
			// return ty;
			// }
			if (ty != null && suffix.equals("*")) {
				ty = Ty.tImArray(ty);
				ODebug.trace("arity %s", ty);
				return ty;
			}
		}
		return ty;
	}

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

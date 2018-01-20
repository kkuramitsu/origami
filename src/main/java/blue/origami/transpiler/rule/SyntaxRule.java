package blue.origami.transpiler.rule;

import blue.origami.common.OArrays;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.type.Ty;

public class SyntaxRule extends LoggerRule implements Symbols {

	public AST[] parseParamNames(Env env, AST params) {
		if (params == null) {
			return OArrays.emptyTrees;
		} else if (params.has(_name)) {
			return new AST[] { params.get(_name) };
		} else {
			AST[] paramNames = new AST[params.size()];
			int i = 0;
			for (AST sub : params) {
				paramNames[i] = sub.get(_name);
				i++;
			}
			return paramNames;
		}
	}

	Ty parseReturnType(Env env, AST type) {
		return this.parseReturnType(env, null, type);
	}

	Ty parseReturnType(Env env, String name, AST type) {
		if (type != null) {
			return env.parseType(env, type, null);
		}
		if (name != null) {
			if (name.endsWith("?")) {
				return Ty.tBool;
			}
		}
		return Ty.tVarParam(name);
	}

	Ty[] parseParamTypes(Env env, AST params) {
		return this.parseParamTypes(env, params, null);
	}

	Ty[] parseParamTypes(Env env, AST params, Ty defaultType) {
		if (params == null) {
			return OArrays.emptyTypes;
		}
		if (params.has(_name)) {
			return new Ty[] { this.parseParamType(env, params.get(_name), params.get(_type), defaultType) };
		}
		Ty[] p = new Ty[params.size()];
		int i = 0;
		for (AST sub : params) {
			p[i] = this.parseParamType(env, sub.get(_name), sub.get(_type), defaultType);
			i++;
		}
		return p;
	}

	private Ty parseParamType(Env env, AST param, AST type, Ty defaultType) {
		String name = param.getString();
		boolean isMutable = name.endsWith("$");
		Ty ty = null;
		if (type != null) {
			ty = env.parseType(env, type, null);
		}
		if (ty == null) {
			if (name.endsWith("?")) {
				ty = Ty.tBool;
			} else {
				NameHint hint = env.findNameHint(env, name);
				if (hint != null) {
					ty = hint.getType();
				} else if (NameHint.isOneLetterName(name)) {
					ty = Ty.tVarParam(name);
				} else if (defaultType != null) {
					ty = defaultType;
				}
			}
		}
		if (ty == null) {
			throw new ErrorCode(param, TFmt.no_type_hint__YY1, param.getString());
		}
		return isMutable ? ty.toMutable() : ty;
	}

	// public Ty parseTypeArity(TEnv env, Ty ty, AST param) {
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

	public Ty[] parseTypes(Env env, AST types) {
		if (types == null) {
			return OArrays.emptyTypes;
		}
		Ty[] p = new Ty[types.size()];
		int i = 0;
		for (AST sub : types) {
			p[i] = env.parseType(env, sub, null);
			i++;
		}
		return p;
	}

}

package blue.origami.transpiler.rule;

import java.util.Arrays;

import blue.origami.common.OArrays;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.type.Ty;
import origami.nez2.ParseTree;
import origami.nez2.Token;

public class SyntaxRule extends LoggerRule implements Symbols {

	public Token[] parseParamNames(Env env, ParseTree params) {
		if (params == null) {
			return OArrays.emptyTokens;
		} else if (params.has(_name)) {
			return new Token[] { env.s(params.get(_name)) };
		} else {
			return Arrays.stream(params.asArray()).map(t -> env.s(t)).toArray(Token[]::new);
		}
	}

	Ty parseReturnType(Env env, ParseTree type) {
		return this.parseReturnType(env, null, type);
	}

	Ty parseReturnType(Env env, String name, ParseTree type) {
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

	Ty[] parseParamTypes(Env env, ParseTree params) {
		return this.parseParamTypes(env, params, null);
	}

	Ty[] parseParamTypes(Env env, ParseTree params, Ty defaultType) {
		if (params == null) {
			return OArrays.emptyTypes;
		}
		if (params.has(_name)) {
			return new Ty[] { this.parseParamType(env, params.get(_name), params.get(_type), defaultType) };
		}
		Ty[] p = new Ty[params.size()];
		int i = 0;
		for (ParseTree sub : params.asArray()) {
			p[i] = this.parseParamType(env, sub.get(_name), sub.get(_type), defaultType);
			i++;
		}
		return p;
	}

	private Ty parseParamType(Env env, ParseTree ns, ParseTree type, Ty defaultType) {
		String name = ns.asString();
		boolean isMutable = NameHint.isMutable(name);
		Ty ty = null;
		if (type != null) {
			ty = env.parseType(env, type, null);
		}
		if (ty == null) {
			if (name.endsWith("?")) {
				ty = Ty.tBool;
			} else {
				ty = env.findNameHint(name);
				if (ty == null) {
					if (NameHint.isOneLetterName(name)) {
						ty = Ty.tVarParam(name);
					} else {
						ty = defaultType;
					}
				}
			}
		}
		if (ty == null) {
			throw new ErrorCode(env.s(ns), TFmt.no_type_hint__YY1, ns.asString());
		}
		return isMutable ? ty.toMutable() : ty;
	}

	public Ty[] parseTypes(Env env, ParseTree types) {
		if (types == null) {
			return OArrays.emptyTypes;
		}
		Ty[] p = new Ty[types.size()];
		int i = 0;
		for (ParseTree sub : types.asArray()) {
			p[i] = env.parseType(env, sub, null);
			i++;
		}
		return p;
	}

}

package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.ConstMap;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DoneCode;
import blue.origami.transpiler.type.FuncTy;
import blue.origami.transpiler.type.Ty;

public class CodeMapDecl implements ParseRule, Symbols {
	@Override
	public Code apply(Env env, AST t) {
		// String name = t.getStringAt(_name, "");
		return this.parseCodeMap(env, t.get(_list));
	}

	public Code parseCodeMap(Env env, AST t) {
		for (AST map : t) {
			String name = map.getStringAt(_name, "");
			String value = map.getStringAt(_value, "");
			if (map.has(_type)) {
				Ty ty = env.parseType(env, map.get(_type), null);
				if (ty.isFunc()) {
					int arrow = parseArrowLevel(name);
					if (arrow == -1) {
						this.parseFuncMap(env, name, (FuncTy) ty, value);
					} else {
						this.parseArrow(env, arrow, (FuncTy) ty, value);
					}
				} else {
					this.parseConst(env, name, ty, value);
				}
			} else {
				if (name.startsWith("'")) {
					this.parseSyntaxMap(env, name.substring(1), value);
				} else {
					this.parseTypeMap(env, name, value);
				}
			}
		}
		return new DoneCode();
	}

	private void parseSyntaxMap(Env env, String key, String value) {
		env.getTranspiler().defineSyntax(key, value);
	}

	private void parseTypeMap(Env env, String name, String value) {
		// return Ty.tBaseTy(name, value);
	}

	private void parseConst(Env env, String name, Ty ty, String value) {
		CodeMap codeMap = new ConstMap(name, value, ty);
		env.addConst(name, codeMap);
	}

	public static int parseArrowLevel(String key) {
		switch (key) {
		case "<---":
			return CodeMap.BADCONV | CodeMap.Faulty;
		case "<--":
			return CodeMap.CONV;
		case "<-":
			return CodeMap.BESTCONV;
		case "<==":
			return CodeMap.CAST;
		case "<=":
			return CodeMap.BESTCAST;
		}
		return -1;
	}

	private void parseArrow(Env env, int acc, FuncTy ty, String value) {
		if (ty.getParamTypes().length == 1) {
			String key = ty.keyMemo();
			CodeMap codeMap = new CodeMap(acc, key, value, ty.getReturnType(), ty.getParamTypes());
			env.addArrow(key, codeMap);
		}
	}

	private void parseFuncMap(Env env, String name, FuncTy ty, String value) {
		CodeMap codeMap = new CodeMap(0, name, value, ty.getReturnType(), ty.getParamTypes());
		env.addCodeMap(name, codeMap);
	}

}
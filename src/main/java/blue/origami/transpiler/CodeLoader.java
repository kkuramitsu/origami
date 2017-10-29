package blue.origami.transpiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import blue.origami.Version;
import blue.origami.common.OArrays;
import blue.origami.common.OConsole;
import blue.origami.transpiler.code.CastCode;
import blue.origami.transpiler.type.Ty;

public class CodeLoader {
	final Transpiler env;
	final String common;
	final String base;
	final String defaul;
	final HashMap<String, String> keyMap = new HashMap<>();

	CodeLoader(Transpiler env) {
		this.env = env;
		String target = env.getTargetName();
		this.base = Version.ResourcePath + "/codemap/" + target + "/";
		this.common = this.base.replace(target, "common");
		this.defaul = this.base.replace(target, "default");
	}

	public String getPath(String file) {
		return this.base + file;
	}

	public void load(String file) {
		try {
			this.load(this.common + file, false);
		} catch (Throwable e) {
		}
		try {
			this.load(this.base + file, false);
		} catch (Throwable e) {
			OConsole.exit(1, e);
		}
		try {
			this.load(this.defaul + file, true);
		} catch (Throwable e) {
		}
	}

	private void load(String path, boolean isDefault) throws Throwable {
		// String path = this.base + file;
		File f = new File(path);
		InputStream s = f.isFile() ? new FileInputStream(path) : CodeLoader.class.getResourceAsStream(path);
		if (s == null) {
			throw new FileNotFoundException(path);
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(s));
		String line = null;
		String key = null;
		String[] requires = OArrays.emptyNames;
		int linenum = 0;
		StringBuilder multiStrings = null;
		String delim = null;
		while ((line = reader.readLine()) != null) {
			linenum += 1;
			if (multiStrings == null) {
				if (line.startsWith("#")) {
					if (line.startsWith("#include")) {
						for (String file : OArrays.ltrim2(line.split("\\S"))) {
							this.load(file);
						}
					}
					if (line.startsWith("#require")) {
						requires = OArrays.ltrim2(line.split("\\S"));
					}
					continue;
				}
				int loc = line.indexOf(" =");
				if (loc <= 0) {
					continue;
				}
				key = line.substring(0, loc).trim();
				String value = line.substring(loc + 2).trim();
				if (this.keyMap.containsKey(key)) {
					System.out.printf("duplicated symbols: %s at %s:%d\n", key, path, linenum);
					key = null;
				} else {
					if (value.length() > 0) {
						this.keyMap.put(key, key);
					}
				}
				if (value.equals("'''") || value.equals("\"\"\"")) {
					delim = value;
					multiStrings = new StringBuilder();
				} else {
					this.defineSymbol(this.env, requires, key, value);
				}
			} else {
				if (line.trim().equals(delim)) {
					this.defineSymbol(this.env, requires, key, multiStrings.toString());
					multiStrings = null;
				} else {
					if (multiStrings.length() > 0) {
						multiStrings.append("\n");
					}
					multiStrings.append(line);
				}
			}
		}
		reader.close();
	}

	void defineSymbol(Env env, String[] requires, String key, String value) {
		if (key == null) {
			return;
		}
		if (key.startsWith("`")) {
			env.getTranspiler().defineSyntax(key.substring(1), value);
		}
		int acc = 0;
		if (key.endsWith("!!")) {
			key = key.substring(0, key.length() - 2);
			acc |= CodeMap.Faulty;
		}
		if (key.endsWith("@")) {
			key = key.substring(0, key.length() - 1);
			acc |= CodeMap.Impure;
		}
		int loc = key.indexOf(':');
		if (loc == -1) {
			String name = key;
			if (key.indexOf('>') > 0) {
				if ((loc = key.indexOf("--->")) > 0) {
					this.addArrow(env, key, value, loc, 4, CastCode.BADCONV | CodeMap.Faulty);
				} else if ((loc = key.indexOf("-->")) > 0) {
					this.addArrow(env, key, value, loc, 3, CastCode.CONV);
				} else if ((loc = key.indexOf("==>")) > 0) {
					this.addArrow(env, key, value, loc, 3, CastCode.CAST);
				} else if ((loc = key.indexOf("->")) > 0) {
					this.addArrow(env, key, value, loc, 2, CastCode.BESTCONV);
				} else if ((loc = key.indexOf("=>")) > 0) {
					this.addArrow(env, key, value, loc, 2, CastCode.BESTCAST);
				}
				return;
			}
			env.add(name, new CodeMap(acc, name, value, Ty.tVoid, OArrays.emptyTypes));
		} else {
			String name = key.substring(0, loc);
			String[] tdescs = key.substring(loc + 1).split(":");
			Ty ret = this.parseType(tdescs[tdescs.length - 1]);
			Ty[] p = OArrays.emptyTypes;
			if (tdescs.length > 1) {
				p = new Ty[tdescs.length - 1];
				for (int i = 0; i < p.length; i++) {
					p[i] = this.parseType(tdescs[i]);
				}
			}
			CodeMap tp = new CodeMap(acc, name, value, ret, p);
			env.add(name, tp);
			// env.add(key, tp);
		}
	}

	void addArrow(Env env, String key, String value, int loc, int len, int acc) {
		Ty f = this.parseType(key.substring(0, loc));
		Ty t = this.parseType(key.substring(loc + len));
		String name = f + "->" + t;
		CodeMap codeMap = new CodeMap(acc, name, value, t, f);
		env.add(name, codeMap);
	}

	Ty parseType(String tdesc) {
		Ty ty = this.env.getType(tdesc);
		if (ty != null) {
			return ty;
		}
		// if (tdesc.startsWith("|")) {
		// Ty[] choice = Arrays.stream(tdesc.substring(1).split("\\|")).map(s ->
		// this.parseType(s)).toArray(Ty[]::new);
		// return new UnionTy(choice);
		// }
		int loc = 0;
		if ((loc = tdesc.indexOf("->")) > 0) {
			int loc2 = tdesc.indexOf(',');
			Ty tt = this.parseType(tdesc.substring(loc + 2));
			if (loc2 > 0) {
				String param = tdesc.substring(0, loc);
				Ty ft1 = this.parseType(param.substring(0, loc2));
				Ty ft2 = this.parseType(param.substring(loc2 + 1));
				return Ty.tFunc(tt, ft1, ft2);
			} else {
				Ty ft = this.parseType(tdesc.substring(0, loc));
				return Ty.tFunc(tt, ft);
			}
		}
		if (tdesc.endsWith("*")) {
			ty = this.parseType(tdesc.substring(0, tdesc.length() - 1));
			return Ty.tList(ty);
		}
		if (tdesc.endsWith("[]")) {
			ty = this.parseType(tdesc.substring(0, tdesc.length() - 2));
			return Ty.tList(ty);
		}
		if (tdesc.endsWith("{}")) {
			ty = this.parseType(tdesc.substring(0, tdesc.length() - 2));
			return Ty.tArray(ty);
		}
		if (tdesc.endsWith("?")) {
			ty = this.parseType(tdesc.substring(0, tdesc.length() - 1));
			return Ty.tOption(ty);
		}
		if (tdesc.endsWith("]")) {
			loc = tdesc.indexOf('[');
			ty = this.parseType(tdesc.substring(loc + 1, tdesc.length() - 1));
			return Ty.tGeneric(tdesc.substring(0, loc), ty);
		}
		if (tdesc.endsWith("}")) {
			loc = tdesc.indexOf('{');
			ty = this.parseType(tdesc.substring(loc + 1, tdesc.length() - 1));
			return Ty.tGeneric(Ty.Mut + tdesc.substring(0, loc), ty);
		}
		ty = getHiddenType(tdesc);
		assert (ty != null) : "undefined '" + tdesc + "'";
		return ty;
	}

	static HashMap<String, Ty> hiddenMap = new HashMap<>();

	public static Ty getHiddenType(String tsig) {
		if (hiddenMap.isEmpty()) {
			hiddenMap.put("()", Ty.tVoid);
			hiddenMap.put("any", Ty.tVarParam[0]);
			hiddenMap.put("byte", Ty.tByte);
			hiddenMap.put("char", Ty.tChar);
			hiddenMap.put("int64", Ty.tInt64);
			hiddenMap.put("a", Ty.tVarParam[0]);
			hiddenMap.put("b", Ty.tVarParam[1]);
			hiddenMap.put("c", Ty.tVarParam[2]);
		}
		return hiddenMap.get(tsig);
	}

	public void addParsedName(String name) {
		// Transpiler tr = env().getTranspiler();
		// if (!NameHint.isOneLetterName(name)) {
		// tr.addParsedName1(name);
		// }
	}

}
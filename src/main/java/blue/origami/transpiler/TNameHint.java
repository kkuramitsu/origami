package blue.origami.transpiler;

import blue.origami.transpiler.code.TCode;
import blue.origami.util.ODebug;

public interface TNameHint {

	public default boolean isNameHint(TEnv env) {
		return true;
	}

	public TType getType();

	public TCode getDefaultValue();

	public static TNameHint newNameHint(TType t) {
		TCode v = t.getDefaultValue();
		return new SimpleNameHint(t, v);
	}

	static TNameHint find(TEnv env, String name) {
		return env.get(name, TNameHint.class, (d, c) -> d.isNameHint(env) ? d : null);
	}

	static TNameHint lookupSubNames(TEnv env, String name) {
		TNameHint t = find(env, name);
		if (t != null) {
			return t;
		}
		for (int loc = 1; loc < name.length() - 2; loc++) {
			String subname = name.substring(loc);
			t = find(env, subname);
			if (t != null) {
				return t;
			}
		}
		return null;
	}

	public static String safeName(String name) {
		return shortName(name.replace('?', 'Q'));
	}

	public static String shortName(String name) {
		int loc = name.length() - 1;
		for (; loc > 0; loc--) {
			char c = name.charAt(loc);
			if (c != '\'' && !Character.isDigit(c) && c != '_') {
				break;
			}
		}
		return name.substring(0, loc + 1);
	}

	public static boolean isShortName(String name) {
		String n = shortName(name);
		ODebug.trace("shortname %s", n);
		return n.length() == 1 && Character.isLowerCase(n.charAt(0));
	}

	public static TNameHint lookupNameHint(TEnv env, String name) {
		int loc = name.length() - 1;
		for (; loc > 0; loc--) {
			char c = name.charAt(loc);
			if (c != '\'' && !Character.isDigit(c) && c != '_') {
				break;
			}
		}
		return lookupSubNames(env, name.substring(0, loc + 1));
	}

}

class SimpleNameHint implements TNameHint {
	TType ty;
	TCode defined;

	SimpleNameHint(TType ty, TCode def) {
		this.ty = ty;
		this.defined = def;
	}

	@Override
	public TType getType() {
		return this.ty;
	}

	@Override
	public TCode getDefaultValue() {
		return this.defined;
	}
}

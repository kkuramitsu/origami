package blue.origami.transpiler;

import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.type.Ty;

public interface NameHint {

	public boolean equalsName(String key);

	public default boolean isLocalOnly() {
		return true;
	}

	public default boolean isGloballyUsed() {
		return false;
	}

	public default boolean useLocal() {
		return false; // first used;
	}

	public default NameHint useGlobal() {
		return this;
	}

	public Ty getType();

	public Code getDefaultValue();

	public static NameHint newNameDecl(String name, Ty t) {
		return new NameDecl(name, t);
	}

	static NameHint find(Env env, boolean globalOnly, String name) {
		if (globalOnly) {
			return env.get(name, NameHint.class, (d, c) -> d.isGloballyUsed() ? d : null);
		} else {
			return env.get(name, NameHint.class);
		}
	}

	static NameHint lookupSubNames(Env env, boolean globalOnly, String name) {
		NameHint t = find(env, globalOnly, name);
		if (t != null) {
			return t;
		}
		for (int loc = 1; loc < name.length() - 2; loc++) {
			String subname = name.substring(loc);
			t = find(env, globalOnly, subname);
			if (t != null) {
				return t;
			}
		}
		return null;
	}

	public static NameHint lookupNameHint(Env env, boolean globalOnly, String name) {
		return lookupSubNames(env, globalOnly, shortName(name));
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

	public static boolean isOneLetterName(String name) {
		String n = shortName(name);
		// ODebug.trace("shortname %s", n);
		return n.length() == 1 && Character.isLowerCase(n.charAt(0));
	}

}

class NameDecl implements NameHint {

	String name;
	Ty ty;
	boolean useGlobal = false;
	boolean useLocal = false;

	NameDecl(String name, Ty ty) {
		this.name = name;
		this.ty = ty;
	}

	@Override
	public boolean isLocalOnly() {
		return false;
	}

	@Override
	public boolean useLocal() {
		boolean b = this.useLocal;
		this.useLocal = true;
		return !b;
	}

	@Override
	public boolean isGloballyUsed() {
		return this.useGlobal;
	}

	@Override
	public NameHint useGlobal() {
		this.useGlobal = true;
		return this;
	}

	@Override
	public boolean equalsName(String name) {
		return this.name.equals(name);
	}

	@Override
	public Ty getType() {
		return this.ty;
	}

	@Override
	public Code getDefaultValue() {
		return this.ty.getDefaultValue();
	}

}

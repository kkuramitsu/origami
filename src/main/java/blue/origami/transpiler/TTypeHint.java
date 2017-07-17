package blue.origami.transpiler;

import blue.origami.util.ODebug;

public interface TTypeHint {

	public default boolean isTypeHint(TEnv env) {
		return true;
	}

	public TType getType(TEnv env);

	public static TTypeHint newTypeHint(TType t) {
		return new TypeHint(t);
	}

	static class TypeHint implements TTypeHint {
		TType defined;

		TypeHint(TType t) {
			this.defined = t;
		}

		@Override
		public TType getType(TEnv env) {
			return defined;
		}
	}

	static TType getType(TEnv env, String name) {
		TTypeHint n = env.get(name, TTypeHint.class, (d, c) -> d.isTypeHint(env) ? d : null);
		if (n != null) {
			return n.getType(env);
		}
		return null;
	}

	static TType lookupSubNames(TEnv env, String name) {
		TType t = getType(env, name);
		if (t != null) {
			return t;
		}
		for (int loc = 1; loc < name.length() - 2; loc++) {
			String subname = name.substring(loc);
			t = getType(env, subname);
			if (t != null) {
				TType p = getType(env, name.substring(0, loc));
				return mergeType(p, t);
			}
		}
		return null;
	}

	static TType mergeType(TType p, TType t) {
		ODebug.trace("merge %s %s", p, t);
		if (p == null) {
			return t;
		}
		// return t.mergeType(p);
		return t;
	}

	public static TType lookupTypeName(TEnv env, String name) {
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

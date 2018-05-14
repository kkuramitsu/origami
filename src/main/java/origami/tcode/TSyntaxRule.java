package origami.tcode;

import blue.origami.common.OFormat;
import blue.origami.common.TLog;
import blue.origami.transpiler.TFmt;
import origami.nez2.ParseTree;
import origami.nez2.Token;

public class TSyntaxRule implements TSyntax {
	public void init(TEnv env) {
		Class<?>[] list = this.getClass().getClasses();
		for (Class<?> c : list) {
			if (c.isAssignableFrom(TRule.class)) {
				try {
					String name = c.getSimpleName().substring(1);
					TRule r = (TRule) c.newInstance();
					env.addBinding(null, name, r);
				} catch (InstantiationException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@SuppressWarnings("serial")
	public static class TSyntaxException extends RuntimeException {
		final TLog log;

		public TSyntaxException(Token s, OFormat fmt, Object... args) {
			this.log = new TLog(s, TLog.Error, fmt, args);
		}
	}

	public static TCode parse(TEnv env, String path, ParseTree t) {
		String name = t.tag();
		TCode node = env.get(name, TRule.class, (d, c) -> d.match(env, path, t));
		if (node == null) {
			throw new TSyntaxException(t.asToken(path), TFmt.undefined_syntax__YY1, name);
		}
		return node;
	}

	public static TCode parseCatch(TEnv env, String path, ParseTree t) {
		try {
			return parse(env, path, t);
		} catch (TSyntaxException e) {
			TLog log = e.log;
			return new TCode(log.s, pError, log, null);
		}
	}

	// value

	public class sInt implements TRule {
		@Override
		public TCode match(TEnv env, String path, ParseTree t) {
			String s = t.asString();
			int n = Integer.parseInt(s);
			return new TCode(t.asToken(path), pInt, n, env.getType(int.class));
		}
	}

	public class sName implements TRule {
		@Override
		public TCode match(TEnv env, String path, ParseTree t) {
			String s = t.asString();
			return new TCode(t.asToken(path), pName, s, null);
		}
	}

	public class sInfix implements TRule {
		@Override
		public TCode match(TEnv env, String path, ParseTree t) {
			ParseTree op = t.get("op");
			TCode left = parse(env, path, t.get("left"));
			TCode right = parse(env, path, t.get("right"));
			return new TCode(op.asToken(path), op.asString(), left, right);
		}
	}

}

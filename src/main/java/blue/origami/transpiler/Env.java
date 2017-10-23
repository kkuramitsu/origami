package blue.origami.transpiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

import blue.origami.Version;
import blue.origami.common.Handled;
import blue.origami.common.ODebug;
import blue.origami.common.OFormat;
import blue.origami.common.SourcePosition;
import blue.origami.common.TLog;
import blue.origami.transpiler.code.CastCode;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.rule.ParseRule;
import blue.origami.transpiler.type.FuncTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarLogger;

public class Env implements TEnvTraits, EnvApi {
	private Env parent;
	private HashMap<String, EnvEntry> definedMap = null;

	public Env(Env parent) {
		this.parent = parent;
	}

	@Override
	public Env getParent() {
		return this.parent;
	}

	@Override
	public EnvEntry getEntry(String name) {
		if (this.definedMap != null) {
			return this.definedMap.get(name);
		}
		return null;
	}

	@Override
	public void addEntry(String name, EnvEntry defined) {
		if (this.definedMap == null) {
			this.definedMap = new HashMap<>();
		}
		EnvEntry prev = this.definedMap.get(name);
		defined.push(prev);
		this.definedMap.put(name, defined);
		// System.out.printf("adding symbol %s %s on %s at env %s\n", name,
		// defined.getHandled(), defined.pop(),
		// this.getClass().getSimpleName());
		// this.hookEntry(name, defined);
	}

	@Override
	public Env env() {
		return this;
	}

}

class EnvEntry implements Handled<Object> {
	private Object value;
	private EnvEntry onstack = null;

	EnvEntry(SourcePosition s, Object value) {
		this.value = value;
	}

	public EnvEntry push(EnvEntry onstack) {
		this.onstack = onstack;
		return this;
	}

	public EnvEntry pop() {
		return this.onstack;
	}

	@Override
	public Object getHandled() {
		return this.value;
	}

	public Object setHandled(Object value) {
		Object v = this.value;
		this.value = value;
		return v;
	}
}

interface TEnvTraits {

	void addEntry(String name, EnvEntry d);

	EnvEntry getEntry(String name);

	TEnvTraits getParent();

	public default Transpiler getTranspiler() {
		if (this instanceof Transpiler) {
			return (Transpiler) this;
		}
		return this.getParent().getTranspiler();
	}

	public default Env newEnv() {
		return new Env((Env) this);
	}

	public default void add(SourcePosition s, String name, Object value) {
		addEntry(name, new EnvEntry(s, value));
	}

	public default void add(String name, Object value) {
		add(SourcePosition.UnknownPosition, name, value);
	}

	public default void add(Class<?> cname, Object value) {
		add(key(cname), value);
	}

	default String key(Class<?> c) {
		return c.getName();
	}

	public default <X> void set(String name, Class<X> c, X value) {
		for (EnvEntry d = this.getEntry(name); d != null; d = d.pop()) {
			X x = d.getHandled(c);
			if (x != null) {
				d.setHandled(value);
				return;
			}
		}
		add(name, value);
	}

	public default <X, Y> Y getLocal(String name, Class<X> c, EnvMatcher<X, Y> f) {
		for (EnvEntry d = this.getEntry(name); d != null; d = d.pop()) {
			X x = d.getHandled(c);
			if (x != null) {
				return f.match(x, c);
			}
		}
		return null;
	}

	public default <X> X getLocal(String name, Class<X> c) {
		return this.getLocal(name, c, (d, c2) -> d);
	}

	public default <X, Y> Y get(String name, Class<X> c, EnvMatcher<X, Y> f) {
		for (TEnvTraits env = this; env != null; env = env.getParent()) {
			Y y = env.getLocal(name, c, f);
			if (y != null) {
				return y;
			}
		}
		return null;
	}

	public default <X> X get(String name, Class<X> c) {
		return this.get(name, c, (d, c2) -> d);
	}

	public default <X> X get(Class<X> c) {
		return this.get(c.getName(), c);
	}

	// public interface EnvChoicer<X> {
	// public X choice(X x, X y);
	// }
	//
	// public default <X, Y> Y find(String name, Class<X> c, EnvMatcher<X, Y> f,
	// EnvChoicer<Y> g, Y start) {
	// Y y = start;
	// for (TEnvTraits env = this; env != null; env = env.getParent()) {
	// for (EnvEntry d = env.getEntry(name); d != null; d = d.pop()) {
	// X x = d.getHandled(c);
	// if (x != null) {
	// Y updated = f.match(x, c);
	// y = g.choice(y, updated);
	// }
	// }
	// }
	// return y;
	// }

	// public interface EnvBreaker<X> {
	// public boolean isEnd(X x);
	// }
	//
	// public default <X, Y> Y find(String name, Class<X> c, EnvMatcher<X, Y> f,
	// TEnvChoicer<Y> g, Y start,
	// EnvBreaker<Y> h) {
	// Y y = start;
	// for (TEnvTraits env = this; env != null; env = env.getParent()) {
	// for (EnvEntry d = env.getEntry(name); d != null; d = d.pop()) {
	// X x = d.getHandled(c);
	// if (x != null) {
	// y = g.choice(y, f.match(x, c));
	// if (h.isEnd(y)) {
	// return y;
	// }
	// }
	// }
	// }
	// return y;
	// }

	public default <X> void findList(String name, Class<X> c, List<X> l, ListMatcher<X> f) {
		for (TEnvTraits env = this; env != null; env = env.getParent()) {
			for (EnvEntry d = env.getEntry(name); d != null; d = d.pop()) {
				X x = d.getHandled(c);
				if (x != null && f.isMatched(x)) {
					l.add(x);
				}
			}
		}
	}

	public default <X> void findList(Class<?> cname, Class<X> c, List<X> l, ListMatcher<X> f) {
		findList(key(cname), c, l, f);
	}

	public default Code catchCode(Supplier<Code> f) {
		try {
			return f.get();
		} catch (ErrorCode e) {
			return e;
		}
	}

	public default void reportLog(TLog log) {
		log.dump();
	}

	public default void reportError(SourcePosition s, OFormat format, Object... args) {
		new TLog(s, TLog.Error, format, args).dump();
	}

	public default void reportWarning(SourcePosition s, OFormat format, Object... args) {
		new TLog(s, TLog.Warning, format, args).dump();
	}

	public default void reportNotice(SourcePosition s, OFormat format, Object... args) {
		new TLog(s, TLog.Notice, format, args).dump();
	}
}

interface EnvApi {
	Env env();

	public default String getSymbolOrElse(String key, String def) {
		CodeMap tp = env().get(key, CodeMap.class);
		return tp == null ? def : tp.getDefined();
	}

	public default String getSymbol(String... keys) {
		for (int i = 0; i < keys.length - 1; i++) {
			String s = this.getSymbolOrElse(keys[i], null);
			if (s != null) {
				return s;
			}
		}
		return keys[keys.length - 1];
	}

	public default CodeMap getTemplate(String... keys) {
		for (int i = 0; i < keys.length - 1; i++) {
			CodeMap tp = env().get(keys[i], CodeMap.class);
			if (tp != null) {
				return tp;
			}
		}
		String last = keys[keys.length - 1];
		return last == null ? null : new CodeMap(last);
	}

	public default String fmt(String... keys) {
		for (int i = 0; i < keys.length - 1; i++) {
			CodeMap tp = env().get(keys[i], CodeMap.class);
			if (tp != null) {
				return tp.getDefined();
			}
		}
		return keys[keys.length - 1];
	}

	public default String format(String key, String def, Object... args) {
		return String.format(this.getSymbolOrElse(key, def), args);
	}

	public default Ty getType(String tsig) {
		return env().get(tsig, Ty.class);
	}

	public default NameHint addNameDecl(Env env, String names, Ty t) {
		return this.addNameDecl(env, names.split(","), t);
	}

	public default NameHint addNameDecl(Env env, String[] names, Ty t) {
		NameHint hint = null;
		for (String n : names) {
			hint = NameHint.newNameDecl(n, t);
			env().add(NameHint.shortName(n), hint);
		}
		return hint;
	}

	public default void addGlobalName(Env env, String name, Ty t) {
		Transpiler tr = env.getTranspiler();
		NameHint hint = NameHint.newNameDecl(name, t).useGlobal();
		tr.add(name, hint);
	}

	public default NameHint findNameHint(Env env, String name) {
		return NameHint.lookupNameHint(env, false, name);
	}

	public default NameHint findGlobalNameHint(Env env, String name) {
		NameHint hint = NameHint.lookupNameHint(env.getTranspiler(), true, name);
		if (hint == null) {
			hint = NameHint.lookupNameHint(env, false, name);
		}
		if (hint != null) {
			if (!hint.equalsName(name) && hint.isLocalOnly()) {
				env.addGlobalName(env, name, hint.getType());
			} else {
				hint.useGlobal();
			}
		}
		return hint;
	}

	public default Code parseCode(Env env, AST t) {
		String name = t.getTag().getSymbol();
		Code node = null;
		try {
			node = env.get(name, ParseRule.class, (d, c) -> d.apply(env, t));
		} catch (ErrorCode e) {
			e.setSource(t);
			// System.out.println(":::" + t);
			throw e;
		}
		if (node == null && env.get(name, ParseRule.class) == null) {
			try {
				Class<?> c = Class.forName(Version.ClassPath + ".transpiler.rule." + name);
				ParseRule rule = (ParseRule) c.newInstance();
				env.getTranspiler().add(name, rule);
				return parseCode(env, t);
			} catch (ClassNotFoundException e) {

			} catch (ErrorCode e) {
				e.setSource(t);
				// System.out.println(":::" + t);
				throw e;
			} catch (Exception e) {
				ODebug.exit(1, e);
			}
		}
		if (node == null) {
			throw new ErrorCode(t, TFmt.undefined_syntax__YY1, name);
		}
		node.setSource(t);
		return node;
	}

	public default Code[] parseSubCode(Env env, AST p) {
		Code[] params = new Code[p.size()];
		for (int i = 0; i < p.size(); i++) {
			params[i] = parseCode(env, p.get(i));
		}
		return params;
	}

	// public default Code[] parseParams(TEnv env, AST t, Symbol param) {
	// AST p = t.get(param);
	// Code[] params = new Code[p.size()];
	// for (int i = 0; i < p.size(); i++) {
	// params[i] = parseCode(env, p.get(i));
	// }
	// return params;
	// }

	public default Ty parseType(Env env, AST t, Supplier<Ty> def) {
		if (t != null) {
			try {
				Code node = parseCode(env, t);
				if (node instanceof TypeCode) {
					return ((TypeCode) node).getTypeValue();
				}
				return node.getType();
			} catch (ErrorCode e) {
				throw e;
			}
		}
		return def.get();
	}

	public default CodeMap findTypeMap(Env env, Ty fromTy0, Ty toTy0) {
		Ty fromTy = fromTy0.finalTy();
		Ty toTy = toTy0.finalTy();
		String key = FuncTy.mapKey(fromTy, toTy);
		CodeMap tp = env.get(key, CodeMap.class);
		if (tp != null) {
			// ODebug.trace("found %s => %s %s", fromTy, toTy, tp);
			return tp;
		}
		tp = fromTy.findMapTo(env, toTy);
		if (tp != null) {
			// ODebug.trace("builtin %s => %s %s", fromTy, toTy, tp);
			env.getTranspiler().add(key, tp);
			return tp;
		}
		tp = toTy.findMapFrom(env, fromTy);
		if (tp != null) {
			// ODebug.trace("builtin %s => %s %s", fromTy, toTy, tp);
			env.getTranspiler().add(key, tp);
			return tp;
		}
		return CodeMap.StupidArrow;
	}

	public default int mapCost(Env env, Ty fromTy0, Ty toTy0, VarLogger logs) {
		if (toTy0.acceptTy(true, fromTy0, logs)) {
			return CastCode.SAME;
		}
		Ty fromTy = fromTy0.finalTy();
		Ty toTy = toTy0.finalTy();
		String key = FuncTy.mapKey(fromTy, toTy);
		CodeMap tp = env.get(key, CodeMap.class);
		if (tp != null) {
			return tp.mapCost();
		}
		int cost = fromTy.costMapTo(env, toTy);
		if (cost < CastCode.STUPID) {
			return cost;
		}
		return toTy.costMapFrom(env, fromTy);
	}

	public default List<CodeMap> findCodeMaps(String name, int paramSize) {
		List<CodeMap> l = new ArrayList<>(8);
		env().findList(name, CodeMap.class, l, (tt) -> !tt.isExpired() && tt.getParamSize() == paramSize);
		return l;
	}

}
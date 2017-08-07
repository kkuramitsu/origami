package blue.origami.transpiler;

import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

import blue.origami.nez.ast.SourcePosition;
import blue.origami.nez.ast.Symbol;
import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.TCastCode;
import blue.origami.transpiler.code.TCastCode.TConvTemplate;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TErrorCode;
import blue.origami.transpiler.code.TTypeCode;
import blue.origami.transpiler.rule.ParseRule;
import blue.origami.util.Handled;
import blue.origami.util.ODebug;

public class TEnv implements TEnvTraits, TEnvApi {
	private TEnv parent;
	private HashMap<String, TEnvEntry> definedMap = null;

	public TEnv(TEnv parent) {
		this.parent = parent;
	}

	@Override
	public TEnv getParent() {
		return this.parent;
	}

	@Override
	public TEnvEntry getEntry(String name) {
		if (this.definedMap != null) {
			return this.definedMap.get(name);
		}
		return null;
	}

	@Override
	public void addEntry(String name, TEnvEntry defined) {
		if (this.definedMap == null) {
			this.definedMap = new HashMap<>();
		}
		TEnvEntry prev = this.definedMap.get(name);
		defined.push(prev);
		this.definedMap.put(name, defined);
		// System.out.printf("adding symbol %s %s on %s at env %s\n", name,
		// defined.getHandled(), defined.pop(),
		// this.getClass().getSimpleName());
		// this.hookEntry(name, defined);
	}

	@Override
	public TEnv env() {
		return this;
	}

}

class TEnvEntry implements Handled<Object> {
	private Object value;
	private TEnvEntry onstack = null;

	TEnvEntry(SourcePosition s, Object value) {
		this.value = value;
	}

	public TEnvEntry push(TEnvEntry onstack) {
		this.onstack = onstack;
		return this;
	}

	public TEnvEntry pop() {
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

	void addEntry(String name, TEnvEntry d);

	TEnvEntry getEntry(String name);

	TEnvTraits getParent();

	public default Transpiler getTranspiler() {
		if (this instanceof Transpiler) {
			return (Transpiler) this;
		}
		return this.getParent().getTranspiler();
	}

	public default TEnv newEnv() {
		return new TEnv((TEnv) this);
	}

	public default void add(SourcePosition s, String name, Object value) {
		addEntry(name, new TEnvEntry(s, value));
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
		for (TEnvEntry d = this.getEntry(name); d != null; d = d.pop()) {
			X x = d.getHandled(c);
			if (x != null) {
				d.setHandled(value);
				return;
			}
		}
		add(name, value);
	}

	public default <X, Y> Y getLocal(String name, Class<X> c, TEnvMatcher<X, Y> f) {
		for (TEnvEntry d = this.getEntry(name); d != null; d = d.pop()) {
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

	public default <X, Y> Y get(String name, Class<X> c, TEnvMatcher<X, Y> f) {
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

	public interface TEnvChoicer<X> {
		public X choice(X x, X y);
	}

	public default <X, Y> Y find(String name, Class<X> c, TEnvMatcher<X, Y> f, TEnvChoicer<Y> g, Y start) {
		Y y = start;
		for (TEnvTraits env = this; env != null; env = env.getParent()) {
			for (TEnvEntry d = env.getEntry(name); d != null; d = d.pop()) {
				X x = d.getHandled(c);
				if (x != null) {
					Y updated = f.match(x, c);
					y = g.choice(y, updated);
				}
			}
		}
		return y;
	}

	public interface TEnvBreaker<X> {
		public boolean isEnd(X x);
	}

	public default <X, Y> Y find(String name, Class<X> c, TEnvMatcher<X, Y> f, TEnvChoicer<Y> g, Y start,
			TEnvBreaker<Y> h) {
		Y y = start;
		for (TEnvTraits env = this; env != null; env = env.getParent()) {
			for (TEnvEntry d = env.getEntry(name); d != null; d = d.pop()) {
				X x = d.getHandled(c);
				if (x != null) {
					y = g.choice(y, f.match(x, c));
					if (h.isEnd(y)) {
						return y;
					}
				}
			}
		}
		return y;
	}

	public default <X> void findList(String name, Class<X> c, List<X> l, ListMatcher<X> f) {
		for (TEnvTraits env = this; env != null; env = env.getParent()) {
			for (TEnvEntry d = env.getEntry(name); d != null; d = d.pop()) {
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

	public default TCode catchCode(Supplier<TCode> f) {
		try {
			return f.get();
		} catch (TErrorCode e) {
			return e;
		}
	}

	public default void reportLog(TLog log) {
		log.dump();
	}

}

interface TEnvApi {
	TEnv env();

	// protected void defineSymbol(String key, String symbol) {
	// if (!this.isDefined(key)) {
	// if (symbol != null) {
	// int s = symbol.indexOf("$|");
	// while (s >= 0) {
	// int e = symbol.indexOf('|', s + 2);
	// String skey = symbol.substring(s + 2, e);
	// // if (this.symbolMap.get(skey) != null) {
	// symbol = symbol.replace("$|" + skey + "|", this.s(skey));
	// // }
	// e = s;
	// s = symbol.indexOf("$|");
	// if (e == s) {
	// break; // avoid infinite looping
	// }
	// // System.out.printf("'%s': %s\n", key, symbol);
	// }
	// }
	// this.symbolMap.put(key, symbol);
	// }
	// }

	public default void defineSymbol(String key, String value) {
		int loc = key.indexOf(':');
		if (loc == -1) {
			String name = key;
			if (key.indexOf('>') > 0) {
				if ((loc = key.indexOf("--->")) > 0) {
					Ty f = this.checkType(key.substring(0, loc));
					Ty t = this.checkType(key.substring(loc + 4));
					name = f + "->" + t;
					env().add(name, new TConvTemplate(name, f, t, TCastCode.BADCONV, value));
					return;
				}
				if ((loc = key.indexOf("-->")) > 0) {
					Ty f = this.checkType(key.substring(0, loc));
					Ty t = this.checkType(key.substring(loc + 3));
					name = f + "->" + t;
					env().add(name, new TConvTemplate(name, f, t, TCastCode.CONV, value));
					return;
				}
				if ((loc = key.indexOf("==>")) > 0) {
					Ty f = this.checkType(key.substring(0, loc));
					Ty t = this.checkType(key.substring(loc + 3));
					name = f + "->" + t;
					env().add(name, new TConvTemplate(name, f, t, TCastCode.CAST, value));
					return;
				}
				if ((loc = key.indexOf("->")) > 0) {
					Ty f = this.checkType(key.substring(0, loc));
					Ty t = this.checkType(key.substring(loc + 2));
					name = f + "->" + t;
					env().add(name, new TConvTemplate(name, f, t, TCastCode.BESTCONV, value));
					return;
				}
				if ((loc = key.indexOf("=>")) > 0) {
					Ty f = this.checkType(key.substring(0, loc));
					Ty t = this.checkType(key.substring(loc + 2));
					name = f + "->" + t;
					env().add(name, new TConvTemplate(name, f, t, TCastCode.BESTCAST, value));
					return;
				}
				System.out.println("FIXME: " + key);
			}
			env().add(name, new TCodeTemplate(name, Ty.tUntyped, TArrays.emptyTypes, value));
		} else {
			String name = key.substring(0, loc);
			String[] tsigs = key.substring(loc + 1).split(":");
			Ty ret = this.checkType(tsigs[tsigs.length - 1]);
			if (tsigs.length > 1) {
				Ty[] p = new Ty[tsigs.length - 1];
				for (int i = 0; i < p.length; i++) {
					p[i] = this.checkType(tsigs[i]);
				}
				env().add(name, new TCodeTemplate(name, ret, p, value));
			} else {
				Template t = new TCodeTemplate(name, ret, TArrays.emptyTypes, value);
				env().add(name, t);
				env().add(key, t);
			}
		}
	}

	public default String getSymbolOrElse(String key, String def) {
		TCodeTemplate tp = env().get(key, TCodeTemplate.class);
		return tp == null ? def : tp.template;
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

	public default Template getTemplate(String... keys) {
		for (int i = 0; i < keys.length - 1; i++) {
			Template tp = env().get(keys[i], Template.class);
			if (tp != null) {
				return tp;
			}
		}
		String last = keys[keys.length - 1];
		return last == null ? null : new TCodeTemplate(last);
	}

	public default String format(String key, String def, Object... args) {
		return String.format(this.getSymbolOrElse(key, def), args);
	}

	public default SourceSection getCurrentSourceSection() {
		return env().getTranspiler().getSourceSection();
	}

	public default void setCurrentSourceSection(SourceSection sec) {
		env().getTranspiler().setSourceSection(sec);
	}

	public default Ty getType(String tsig) {
		return env().get(tsig, Ty.class);
	}

	default Ty checkType(String tsig) {
		Ty t = this.getType(tsig);
		if (t == null) {
			if (tsig.endsWith("*")) {
				t = checkType(tsig.substring(0, tsig.length() - 1));
				return Ty.tImArray(t);
			}
			if (tsig.endsWith("?")) {
				t = checkType(tsig.substring(0, tsig.length() - 1));
				return Ty.tOption(t);
			}
			if (tsig.startsWith("{") && tsig.endsWith("*}")) {
				t = checkType(tsig.substring(1, tsig.length() - 2));
				return Ty.tMArray(t);
			}
			if (tsig.startsWith("Dict[") && tsig.endsWith("]")) {
				t = checkType(tsig.substring(5, tsig.length() - 1));
				return Ty.tImDict(t);
			}
			if (tsig.startsWith("Dict{") && tsig.endsWith("}")) {
				t = checkType(tsig.substring(5, tsig.length() - 1));
				return Ty.tMDict(t);
			}
			if (tsig.startsWith("Option[") && tsig.endsWith("]")) {
				t = checkType(tsig.substring(7, tsig.length() - 1));
				return Ty.tOption(t);
			}
			int loc = 0;
			if ((loc = tsig.indexOf("->")) > 0) {
				Ty ft = checkType(tsig.substring(0, loc));
				Ty tt = checkType(tsig.substring(loc + 2));
				return Ty.tFunc(tt, ft);
			}
			t = Ty.getHidden(tsig);
		}
		assert (t != null) : tsig;
		return t;
	}

	public default void addTypeHint(TEnv env, String names, Ty t) {
		this.addTypeHint(env, names.split(","), t);
	}

	public default void addTypeHint(TEnv env, String[] names, Ty t) {
		if (!t.isUntyped()) {
			TNameHint hint = TNameHint.newNameHint(t);
			for (String n : names) {
				env().add(TNameHint.shortName(n), hint);
			}
		}
	}

	public default TNameHint findNameHint(TEnv env, String name) {
		return TNameHint.lookupNameHint(env, name);
	}

	public default TCode parseCode(TEnv env, Tree<?> t) {
		String name = t.getTag().getSymbol();
		TCode node = null;
		try {
			node = env.get(name, ParseRule.class, (d, c) -> d.apply(env, t));
		} catch (TErrorCode e) {
			e.setSource(t);
			throw e;
		}
		if (node == null && env.get(name, ParseRule.class) == null) {
			try {
				Class<?> c = Class.forName("blue.origami.transpiler.rule." + name);
				ParseRule rule = (ParseRule) c.newInstance();
				env.getTranspiler().add(name, rule);
				return parseCode(env, t);
			} catch (ClassNotFoundException e) {

			} catch (TErrorCode e) {
				throw e;
			} catch (Exception e) {
				ODebug.exit(1, e);
			}
		}
		if (node == null) {
			throw new TErrorCode(t, TFmt.undefined_syntax__YY0, name);
		}
		node.setSource(t);
		return node;
	}

	// public default TCode typeExpr(TEnv env, Tree<?> t) {
	// // if (t == null) {
	// // return new EmptyCode(env);
	// // }
	// return parseCode(env, t);
	// }

	// public default TCode[] typeParams(TEnv env, Tree<?> t) {
	// return typeParams(env, t, OSymbols._param);
	// }

	public default TCode[] parseParams(TEnv env, Tree<?> t, Symbol param) {
		Tree<?> p = t.get(param, null);
		TCode[] params = new TCode[p.size()];
		for (int i = 0; i < p.size(); i++) {
			params[i] = parseCode(env, p.get(i));
		}
		return params;
	}

	public default Ty parseType(TEnv env, Tree<?> t, Ty defty) {
		if (t != null) {
			try {
				TCode node = parseCode(env, t);
				if (node instanceof TTypeCode) {
					return ((TTypeCode) node).getTypeValue();
				}
				return node.getType();
			} catch (TErrorCode e) {
				if (defty == null) {
					throw e;
				}
			}
		}
		return defty;
	}

	public default TConvTemplate findTypeMap(TEnv env, Ty f, Ty t) {
		String key = f + "->" + t;
		TConvTemplate tp = env.get(key, TConvTemplate.class);
		// System.out.printf("FIXME: finding %s %s\n", key, tp);
		if (tp == null && t == Ty.tVoid) {
			String format = env.getSymbol("(Void)", "(void)%s");
			tp = new TConvTemplate("", Ty.tUntyped, Ty.tVoid, TCastCode.SAME, format);
			env.getTranspiler().add(key, tp);
		}
		return tp == null ? TConvTemplate.Stupid : tp;
	}

}
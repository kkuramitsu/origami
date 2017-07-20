package blue.origami.transpiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import blue.origami.nez.ast.SourcePosition;
import blue.origami.nez.ast.Symbol;
import blue.origami.nez.ast.Tree;
import blue.origami.rule.OFmt;
import blue.origami.transpiler.code.TCastCode;
import blue.origami.transpiler.code.TCastCode.TConvTemplate;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TErrorCode;
import blue.origami.transpiler.code.TParamCode;
import blue.origami.transpiler.code.TTypeCode;
import blue.origami.transpiler.rule.TTypeRule;
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

	public interface OListMatcher<X> {
		public boolean isMatched(X x);
	}

	public default <X> void findList(String name, Class<X> c, List<X> l, OListMatcher<X> f) {
		for (TEnvTraits env = this; env != null; env = env.getParent()) {
			for (TEnvEntry d = env.getEntry(name); d != null; d = d.pop()) {
				X x = d.getHandled(c);
				if (x != null && f.isMatched(x)) {
					l.add(x);
				}
			}
		}
	}

	public default <X> void findList(Class<?> cname, Class<X> c, List<X> l, OListMatcher<X> f) {
		findList(key(cname), c, l, f);
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
				if ((loc = key.indexOf("-->>")) > 0) {
					TType f = this.checkType(key.substring(0, loc));
					TType t = this.checkType(key.substring(loc + 4));
					name = f + "->" + t;
					env().add(name, new TConvTemplate(name, f, t, TCastCode.CONV, value));
					return;
				}
				if ((loc = key.indexOf("-->")) > 0) {
					TType f = this.checkType(key.substring(0, loc));
					TType t = this.checkType(key.substring(loc + 3));
					name = f + "->" + t;
					env().add(name, new TConvTemplate(name, f, t, TCastCode.CAST, value));
					return;
				}
				if ((loc = key.indexOf("->>")) > 0) {
					TType f = this.checkType(key.substring(0, loc));
					TType t = this.checkType(key.substring(loc + 3));
					name = f + "->" + t;
					env().add(name, new TConvTemplate(name, f, t, TCastCode.BESTCONV, value));
					return;
				}
				if ((loc = key.indexOf("->")) > 0) {
					TType f = this.checkType(key.substring(0, loc));
					TType t = this.checkType(key.substring(loc + 2));
					name = f + "->" + t;
					env().add(name, new TConvTemplate(name, f, t, TCastCode.BESTCAST, value));
					return;
				}
				System.out.println("FIXME: " + key);
			}
			env().add(name, new TCodeTemplate(name, TType.tUntyped, TConsts.emptyTypes, value));
		} else {
			String name = key.substring(0, loc);
			String[] tsigs = key.substring(loc + 1).split(":");
			TType ret = this.checkType(tsigs[tsigs.length - 1]);
			if (tsigs.length > 1) {
				TType[] p = new TType[tsigs.length - 1];
				for (int i = 0; i < p.length; i++) {
					p[i] = this.checkType(tsigs[i]);
				}
				env().add(name, new TCodeTemplate(name, ret, p, value));
			} else {
				TSkeleton t = new TCodeTemplate(name, ret, TConsts.emptyTypes, value);
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

	public default TSkeleton getTemplate(String... keys) {
		for (int i = 0; i < keys.length - 1; i++) {
			TSkeleton tp = env().get(keys[i], TSkeleton.class);
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

	public default TType getType(String tsig) {
		return env().get(tsig, TType.class);
	}

	default TType checkType(String tsig) {
		TType t = this.getType(tsig);
		assert (t != null) : tsig;
		return t;
	}

	public default void addTypeHint(TEnv env, String names, TType t) {
		TTypeHint hint = TTypeHint.newTypeHint(t);
		for (String n : names.split(",")) {
			env().add(n, hint);
		}
	}

	public default TType lookupTypeHint(TEnv env, String name) {
		return TTypeHint.lookupTypeName(env, name);
	}

	public default TCode typeTree(TEnv env, Tree<?> t) {
		String name = t.getTag().getSymbol();
		TCode node = null;
		try {
			node = env.get(name, TTypeRule.class, (d, c) -> d.apply(env, t));
		} catch (TErrorCode e) {
			e.setSourcePosition(t);
			throw e;
		}
		if (node == null && env.get(name, TTypeRule.class) == null) {
			try {
				Class<?> c = Class.forName("blue.origami.transpiler.rule." + name);
				TTypeRule rule = (TTypeRule) c.newInstance();
				env.getTranspiler().add(name, rule);
				return typeTree(env, t);
			} catch (TErrorCode e) {
				throw e;
			} catch (Exception e) {
				ODebug.traceException(e);
			}
		}
		if (node == null) {
			throw new TErrorCode(t, OFmt.undefined_syntax__YY0, name);
		}
		node.setSourcePosition(t);
		return node;
	}

	public default TCode typeExpr(TEnv env, Tree<?> t) {
		// if (t == null) {
		// return new EmptyCode(env);
		// }
		return typeTree(env, t);
	}

	// public default TCode[] typeParams(TEnv env, Tree<?> t) {
	// return typeParams(env, t, OSymbols._param);
	// }

	public default TCode[] typeParams(TEnv env, Tree<?> t, Symbol param) {
		Tree<?> p = t.get(param, null);
		TCode[] params = new TCode[p.size()];
		for (int i = 0; i < p.size(); i++) {
			params[i] = typeExpr(env, p.get(i));
		}
		return params;
	}

	public default TType parseType(TEnv env, Tree<?> t, TType defty) {
		if (t != null) {
			try {
				TCode node = typeTree(env, t);
				if (node instanceof TTypeCode) {
					return ((TTypeCode) node).getTypeValue();
				}
				return node.getType();
			} catch (TErrorCode e) {
			}
		}
		return defty;
	}

	public default TConvTemplate findTypeMap(TEnv env, TType f, TType t) {
		String key = f + "->" + t;
		TConvTemplate tp = env.get(key, TConvTemplate.class);
		// System.out.printf("FIXME: finding %s %s\n", key, tp);
		return tp == null ? TConvTemplate.Stupid : tp;
	}

	public default TCode findParamCode(TEnv env, String name, TCode... params) {
		// for (TCode p : params) {
		// if (p.isUntyped()) {
		// return new TUntypedParamCode(name, params);
		// }
		// }
		List<TSkeleton> l = new ArrayList<>(8);
		env.findList(name, TSkeleton.class, l, (tt) -> tt.isEnabled() && tt.getParamSize() == params.length);
		// ODebug.trace("l = %s", l);
		if (l.size() == 0) {
			throw new TErrorCode("undefined %s%s", name, types(params));
		}
		TParamCode start = l.get(0).newParamCode(env, name, params);
		int mapCost = start.checkParam(env);
		// System.out.println("cost=" + mapCost + ", " + l.get(0));
		for (int i = 1; i < l.size(); i++) {
			if (mapCost <= 0) {
				return start;
			}
			TParamCode next = l.get(i).newParamCode(env, name, params);
			int nextCost = next.checkParam(env);
			// System.out.println("nextcost=" + nextCost + ", " + l.get(i));
			if (nextCost < mapCost) {
				start = next;
				mapCost = nextCost;
			}
		}
		if (mapCost >= TCastCode.STUPID) {
			// ODebug.trace("miss cost=%d %s", start.getMatchCost(), start);
			throw new TErrorCode("mismatched %s%s", name, types(params));
		}
		return start;
	}

	default String types(TCode... params) {
		StringBuilder sb = new StringBuilder();
		for (TCode t : params) {
			sb.append(" ");
			sb.append(t.getType());
		}
		return sb.toString();
	}

}
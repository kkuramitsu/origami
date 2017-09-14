package blue.origami.transpiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

import blue.origami.nez.ast.LocaleFormat;
import blue.origami.nez.ast.SourcePosition;
import blue.origami.nez.ast.Symbol;
import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.CastCode;
import blue.origami.transpiler.code.CastCode.TConvTemplate;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.rule.ParseRule;
import blue.origami.transpiler.type.FuncTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarDomain;
import blue.origami.transpiler.type.VarLogger;
import blue.origami.transpiler.type.VarTy;
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

	public default void reportError(SourcePosition s, LocaleFormat format, Object... args) {
		new TLog(s, TLog.Error, format, args).dump();
	}

	public default void reportWarning(SourcePosition s, LocaleFormat format, Object... args) {
		new TLog(s, TLog.Warning, format, args).dump();
	}

	public default void reportNotice(SourcePosition s, LocaleFormat format, Object... args) {
		new TLog(s, TLog.Notice, format, args).dump();
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
		boolean pure = true;
		boolean faulty = false;
		if (key.endsWith("!!")) {
			key = key.substring(0, key.length() - 2);
			faulty = true;
		}
		if (key.endsWith("@")) {
			key = key.substring(0, key.length() - 1);
			pure = false;
		}
		int loc = key.indexOf(':');
		if (loc == -1) {
			String name = key;
			if (key.indexOf('>') > 0) {
				if ((loc = key.indexOf("--->")) > 0) {
					Ty f = this.checkType(key.substring(0, loc));
					Ty t = this.checkType(key.substring(loc + 4));
					name = f + "->" + t;
					Template tp = new TConvTemplate(name, f, t, CastCode.BADCONV, value);
					env().add(name, tp);
					tp.asFaulty(faulty);
					return;
				}
				if ((loc = key.indexOf("-->")) > 0) {
					Ty f = this.checkType(key.substring(0, loc));
					Ty t = this.checkType(key.substring(loc + 3));
					name = f + "->" + t;
					env().add(name, new TConvTemplate(name, f, t, CastCode.CONV, value));
					return;
				}
				if ((loc = key.indexOf("==>")) > 0) {
					Ty f = this.checkType(key.substring(0, loc));
					Ty t = this.checkType(key.substring(loc + 3));
					name = f + "->" + t;
					env().add(name, new TConvTemplate(name, f, t, CastCode.CAST, value));
					return;
				}
				if ((loc = key.indexOf("->")) > 0) {
					Ty f = this.checkType(key.substring(0, loc));
					Ty t = this.checkType(key.substring(loc + 2));
					name = f + "->" + t;
					env().add(name, new TConvTemplate(name, f, t, CastCode.BESTCONV, value));
					return;
				}
				if ((loc = key.indexOf("=>")) > 0) {
					Ty f = this.checkType(key.substring(0, loc));
					Ty t = this.checkType(key.substring(loc + 2));
					name = f + "->" + t;
					env().add(name, new TConvTemplate(name, f, t, CastCode.BESTCAST, value));
					return;
				}
				System.out.println("FIXME: " + key);
			}
			env().add(name, new CodeTemplate(name, Ty.tVoid, TArrays.emptyTypes, value));
		} else {
			String name = key.substring(0, loc);
			String[] tsigs = key.substring(loc + 1).split(":");
			Ty ret = this.checkType(tsigs[tsigs.length - 1]);
			if (tsigs.length > 1) {
				Ty[] p = new Ty[tsigs.length - 1];
				for (int i = 0; i < p.length; i++) {
					p[i] = this.checkType(tsigs[i]);
				}
				Template tp = new CodeTemplate(name, ret, p, value);
				env().add(name, tp);
				tp.asFaulty(faulty);
				tp.asPure(pure);
			} else {
				Template tp = new CodeTemplate(name, ret, TArrays.emptyTypes, value);
				env().add(name, tp);
				env().add(key, tp);
				tp.asFaulty(faulty);
				tp.asPure(pure);
			}
		}
	}

	public default String getSymbolOrElse(String key, String def) {
		CodeTemplate tp = env().get(key, CodeTemplate.class);
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
		return last == null ? null : new CodeTemplate(last);
	}

	public default String fmt(String... keys) {
		for (int i = 0; i < keys.length - 1; i++) {
			Template tp = env().get(keys[i], Template.class);
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

	default Ty checkType(String tsig) {
		Ty ty = this.getType(tsig);
		if (ty == null) {
			int loc = 0;
			if ((loc = tsig.indexOf("->")) > 0) {
				int loc2 = tsig.indexOf(',');
				Ty tt = checkType(tsig.substring(loc + 2));
				if (loc2 > 0) {
					String param = tsig.substring(0, loc);
					Ty ft1 = checkType(param.substring(0, loc2));
					Ty ft2 = checkType(param.substring(loc2 + 1));
					return Ty.tFunc(tt, ft1, ft2);
				} else {
					Ty ft = checkType(tsig.substring(0, loc));
					return Ty.tFunc(tt, ft);
				}
			}
			if (tsig.endsWith("*")) {
				ty = checkType(tsig.substring(0, tsig.length() - 1));
				return Ty.tList(ty);
			}
			if (tsig.endsWith("[]")) {
				ty = checkType(tsig.substring(0, tsig.length() - 2));
				return Ty.tArray(ty);
			}
			if (tsig.endsWith("?")) {
				ty = checkType(tsig.substring(0, tsig.length() - 1));
				return Ty.tOption(ty);
			}
			if (tsig.endsWith("]")) {
				loc = tsig.indexOf('[');
				ty = checkType(tsig.substring(loc + 1, tsig.length() - 1));
				return Ty.tMonad(tsig.substring(0, loc), ty);
			}
			ty = getHiddenType(tsig);
		}
		assert (ty != null) : "undefined '" + tsig + "'";
		return ty;
	}

	static HashMap<String, Ty> hiddenMap = new HashMap<>();

	public static Ty getHiddenType(String tsig) {
		if (hiddenMap.isEmpty()) {
			hiddenMap.put("()", Ty.tVoid);
			hiddenMap.put("any", Ty.tAnyRef);
			hiddenMap.put("byte", Ty.tByte);
			hiddenMap.put("char", Ty.tChar);
			hiddenMap.put("int64", Ty.tInt64);
			hiddenMap.put("a", VarDomain.var(0));
			hiddenMap.put("b", new VarTy("b", 1));
			hiddenMap.put("c", new VarTy("c", 2));
		}
		return hiddenMap.get(tsig);
	}

	public default void addParsedName(String name) {
		// Transpiler tr = env().getTranspiler();
		// if (!NameHint.isOneLetterName(name)) {
		// tr.addParsedName1(name);
		// }
	}

	public default NameHint addNameDecl(TEnv env, String names, Ty t) {
		return this.addNameDecl(env, names.split(","), t);
	}

	public default NameHint addNameDecl(TEnv env, String[] names, Ty t) {
		NameHint hint = null;
		for (String n : names) {
			hint = NameHint.newNameDecl(n, t);
			env().add(NameHint.shortName(n), hint);
		}
		return hint;
	}

	public default void addGlobalName(TEnv env, String name, Ty t) {
		Transpiler tr = env.getTranspiler();
		NameHint hint = NameHint.newNameDecl(name, t).useGlobal();
		tr.add(name, hint);
	}

	public default NameHint findNameHint(TEnv env, String name) {
		return NameHint.lookupNameHint(env, false, name);
	}

	public default NameHint findGlobalNameHint(TEnv env, String name) {
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

	public default Code parseCode(TEnv env, Tree<?> t) {
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
				Class<?> c = Class.forName("blue.origami.transpiler.rule." + name);
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

	public default Code[] parseParams(TEnv env, Tree<?> t, Symbol param) {
		Tree<?> p = t.get(param, null);
		Code[] params = new Code[p.size()];
		for (int i = 0; i < p.size(); i++) {
			params[i] = parseCode(env, p.get(i));
		}
		return params;
	}

	public default Ty parseType(TEnv env, Tree<?> t, Supplier<Ty> def) {
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

	public default Template findTypeMap(TEnv env, Ty fromTy0, Ty toTy0) {
		Ty fromTy = fromTy0.finalTy();
		Ty toTy = toTy0.finalTy();
		String key = FuncTy.mapKey(fromTy, toTy);
		Template tp = env.get(key, Template.class);
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
		return TConvTemplate.Stupid;
	}

	public default int mapCost(TEnv env, Ty fromTy0, Ty toTy0, VarLogger logs) {
		if (toTy0.acceptTy(true, fromTy0, logs)) {
			return CastCode.SAME;
		}
		Ty fromTy = fromTy0.finalTy();
		Ty toTy = toTy0.finalTy();
		String key = FuncTy.mapKey(fromTy, toTy);
		Template tp = env.get(key, Template.class);
		if (tp != null) {
			return tp.mapCost();
		}
		int cost = fromTy.costMapTo(env, toTy);
		if (cost < CastCode.STUPID) {
			return cost;
		}
		return toTy.costMapFrom(env, fromTy);
	}

	public default List<Template> findTemplates(String name, int paramSize) {
		List<Template> l = new ArrayList<>(8);
		env().findList(name, Template.class, l, (tt) -> !tt.isExpired() && tt.getParamSize() == paramSize);
		return l;
	}

}
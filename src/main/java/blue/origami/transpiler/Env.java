package blue.origami.transpiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

import blue.origami.Version;
import blue.origami.common.OArrays;
import blue.origami.common.ODebug;
import blue.origami.common.OFormat;
import blue.origami.common.OStrings;
import blue.origami.common.SourcePosition;
import blue.origami.common.TLog;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.FuncCode;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.rule.ParseRule;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.TypeMatcher;
import blue.origami.transpiler.type.VarDomain;

public class Env implements EnvAPIs, EnvApi {
	private Env parent;
	protected Language lang;
	private HashMap<String, Binding> bindMap = null;

	public Env(Env parent) {
		this.parent = parent;
		if (this.parent != null) {
			this.lang = parent.getLanguage();
		}
	}

	@Override
	public Env getParent() {
		return this.parent;
	}

	public Language getLanguage() {
		return this.lang;
	}

	@Override
	public Binding getBinding(String name) {
		if (this.bindMap != null) {
			return this.bindMap.get(name);
		}
		return null;
	}

	@Override
	public void addBinding(String name, Binding defined) {
		if (this.bindMap == null) {
			this.bindMap = new HashMap<>();
		}
		Binding prev = this.bindMap.get(name);
		defined.push(prev);
		this.bindMap.put(name, defined);
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

interface EnvAPIs {

	void addBinding(String name, Binding d);

	Binding getBinding(String name);

	Env getParent();

	public default void add(SourcePosition s, String name, Object value) {
		addBinding(name, new Binding(s, value));
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
		for (Binding d = this.getBinding(name); d != null; d = d.pop()) {
			X x = d.getHandled(c);
			if (x != null) {
				d.setHandled(value);
				return;
			}
		}
		add(name, value);
	}

	public default <X, Y> Y getLocal(String name, Class<X> c, EnvMatcher<X, Y> f) {
		for (Binding d = this.getBinding(name); d != null; d = d.pop()) {
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
		for (EnvAPIs env = this; env != null; env = env.getParent()) {
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

	public default <X> void findList(String name, Class<X> c, List<X> l, ListMatcher<X> f) {
		for (EnvAPIs env = this; env != null; env = env.getParent()) {
			for (Binding d = env.getBinding(name); d != null; d = d.pop()) {
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

	public default void reportError(Code code, OFormat format, Object... args) {
		this.reportLog(new TLog(code.getSource(), TLog.Error, format, args));
	}

	public default void reportError(SourcePosition s, OFormat format, Object... args) {
		this.reportLog(new TLog(s, TLog.Error, format, args));
	}

	public default void reportWarning(SourcePosition s, OFormat format, Object... args) {
		this.reportLog(new TLog(s, TLog.Warning, format, args));
	}

	public default void reportNotice(SourcePosition s, OFormat format, Object... args) {
		this.reportLog(new TLog(s, TLog.Notice, format, args));
	}

	public default Transpiler getTranspiler() {
		if (this instanceof Transpiler) {
			return (Transpiler) this;
		}
		return this.getParent().getTranspiler();
	}

	public default Env newEnv() {
		return new Env((Env) this);
	}

	public default FuncEnv getFuncEnv() {
		if (this instanceof FuncEnv) {
			return (FuncEnv) this;
		}
		return this.getParent().getFuncEnv();
	}

	public default FuncEnv newFuncEnv() { // TopLevel
		return new FuncEnv((Env) this, null, "", OArrays.emptyTrees, OArrays.emptyTypes, Ty.tVarParam[0]);
	}

	public default FuncEnv newFuncEnv(String name, AST[] paramNames, Ty[] paramTypes, Ty returnType) {
		return new FuncEnv((Env) this, null, name, paramNames, paramTypes, returnType);
	}

	public default FuncEnv newLambdaEnv(FuncCode fc) {
		return new FuncEnv((Env) this, this.getFuncEnv(), "", fc.getParamNames(), fc.getParamTypes(),
				fc.getReturnType());
	}

}

interface EnvApi {
	Env env();

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
			env.add(NameHint.shortName(n), hint);
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

	public default void addConst(String name, CodeMap cmap) {
		env().add(name, cmap);
	}

	public default void addArrow(String name, CodeMap cmap) {
		env().add(name, cmap);
		if (cmap.mapCost() < CodeMap.BADCONV) {
			String key = cmap.getParamTypes()[0].keyFrom() + "->";
			env().add(key, cmap);
		}
	}

	public default void addCodeMap(String name, CodeMap cmap) {
		// if (cmap.isAbstract()) {
		// name = " " + name;
		// }
		env().add(name, cmap);
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

	public default CodeMap getArrow(Env env, Ty fromTy, Ty toTy) {
		String key = Ty.mapKey2(fromTy, toTy);
		return env.get(key, CodeMap.class);
	}

	public default CodeMap findArrow(Env env, Ty fromTy, Ty toTy) {
		String key = Ty.mapKey2(fromTy, toTy);
		CodeMap tp = env.get(key, CodeMap.class);
		if (tp != null) {
			return tp;
		}
		fromTy = fromTy.memoed();
		toTy = toTy.memoed();
		tp = fromTy.findMapThisTo(env, fromTy, toTy);
		if (tp != null) {
			// ODebug.trace("builtin %s => %s %s", fromTy, toTy, tp);
			env.getTranspiler().add(key, tp);
			return tp;
		}
		tp = toTy.findMapFromToThis(env, fromTy, toTy);
		if (tp != null) {
			// ODebug.trace("builtin %s => %s %s", fromTy, toTy, tp);
			env.getTranspiler().add(key, tp);
			return tp;
		}
		// findArrows(fromTy, toTy);
		return CodeMap.StupidArrow;
	}

	public default CodeMap genArrow(Ty fromTy, Ty toTy) {
		List<ArrowPair> results = new ArrayList<>();
		for (int depth = 1; depth < 3; depth++) {
			findArrowChain(new ArrowPair(fromTy), fromTy, toTy, depth, results);
			if (results.size() == 1) {
				System.out.println("::: " + results);
				return results.get(0).map();
			}
			if (results.size() > 1) {
				System.out.println("TOO MANY ::: " + results);
				break;
			}
		}
		ODebug.trace("genArrow %s %s NULL", fromTy, toTy);
		return null;
	}

	static class ArrowPair implements OStrings {
		ArrowPair prev;
		CodeMap cmap;
		Ty ty;

		ArrowPair(ArrowPair prev, CodeMap next, Ty ty) {
			this.prev = prev;
			this.cmap = next;
			this.ty = ty;
		}

		ArrowPair(Ty ty) {
			this.prev = null;
			this.cmap = null;
			this.ty = ty;
		}

		ArrowPair then(CodeMap next, Ty ty) {
			return new ArrowPair(this, next, ty);
		}

		@Override
		public void strOut(StringBuilder sb) {
			if (prev != null) {
				prev.strOut(sb);
				sb.append("->");
			}
			this.ty.strOut(sb);
		}

		public CodeMap map() {
			ArrayList<CodeMap> l = new ArrayList<>();
			push(l);
			if (l.size() == 1) {
				return l.get(0);
			}
			return new ArrowMap(toString(), l.toArray(new CodeMap[l.size()]));
		}

		void push(ArrayList<CodeMap> l) {
			if (prev != null) {
				push(l);
			}
			if (cmap != null) {
				l.add(cmap);
			}
		}

	}

	default void findArrowChain(ArrowPair prev, Ty fromTy, Ty toTy, int depth, List<ArrowPair> results) {
		if (depth > 0) {
			List<CodeMap> l = new ArrayList<>(8);
			String key = fromTy.keyFrom() + "->";
			env().findList(key, CodeMap.class, l, (tt) -> !tt.isExpired());
			ODebug.trace("%s :: %s", key, l);
			for (CodeMap cmap : l) {
				Ty ret = cmap.getReturnType();
				if (cmap.isGeneric()) {
					VarDomain dom = new VarDomain(cmap.getParamTypes());
					Ty[] p = dom.conv(cmap.getParamTypes());
					if (!p[0].match(fromTy)) {
						continue;
					}
					ret = dom.conv(cmap.getReturnType()).memoed();
				} else {
					if (!cmap.getParamTypes()[0].match(fromTy)) {
						continue;
					}
				}
				if (ret == toTy) {
					results.add(prev.then(cmap, toTy));
				} else {
					findArrowChain(prev.then(cmap, ret), ret, toTy, depth - 1, results);
				}
			}
		}
	}

	public default int arrowCost(Env env, Ty fromTy, Ty toTy, TypeMatcher logs) {
		if (toTy.match(true, fromTy, logs)) {
			return CodeMap.SAME;
		}
		String key = Ty.mapKey2(fromTy, toTy);
		CodeMap tp = env.get(key, CodeMap.class);
		if (tp != null) {
			return tp.mapCost();
		}
		fromTy = fromTy.memoed();
		toTy = toTy.memoed();
		// tp = genArrow(fromTy, toTy);
		// if (tp != null) {
		// return tp.mapCost();
		// }
		int cost = fromTy.costMapThisTo(env, fromTy, toTy);
		if (cost < CodeMap.STUPID) {
			return cost;
		}
		return toTy.costMapFromToThis(env, fromTy, toTy);
	}

	public default List<CodeMap> findCodeMaps(String name, int paramSize) {
		List<CodeMap> l = new ArrayList<>(8);
		env().findList(name, CodeMap.class, l, (tt) -> !tt.isExpired() && tt.getParamSize() == paramSize);
		return l;
	}

}

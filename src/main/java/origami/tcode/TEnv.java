package origami.tcode;

import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import blue.origami.common.Handled;
import blue.origami.common.OFormat;
import blue.origami.common.TLog;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import origami.nez2.Token;

public class TEnv {
	private TEnv parent;
	private HashMap<String, Binding> bindMap = null;

	public TEnv(TEnv parent) {
		this.parent = parent;
	}

	public TEnv getParent() {
		return this.parent;
	}

	public Binding getBinding(String name) {
		if (this.bindMap != null) {
			return this.bindMap.get(name);
		}
		return null;
	}

	public void addBinding(Token s, String name, Object value) {
		if (this.bindMap == null) {
			this.bindMap = new HashMap<>();
		}
		Binding prev = this.bindMap.get(name);
		Binding defined = new Binding(this, s, name, value, prev);
		this.bindMap.put(name, defined);
		// System.out.printf("adding symbol %s %s on %s at env %s\n", name,
		// defined.getHandled(), defined.pop(),
		// this.getClass().getSimpleName());
		// this.hookEntry(name, defined);
	}

	public void add(String name, Object value) {
		this.addBinding(null, name, value);
	}

	public void add(Class<?> cname, Object value) {
		this.add(this.key(cname), value);
	}

	String key(Class<?> c) {
		return c.getName();
	}

	public <X> void set(String name, Class<X> c, X value) {
		for (Binding d = this.getBinding(name); d != null; d = d.pop()) {
			X x = d.getHandled(c);
			if (x != null) {
				d.setHandled(value);
				return;
			}
		}
		this.add(name, value);
	}

	public <X, Y> Y getLocal(String name, Class<X> c, EnvMatcher<X, Y> f) {
		for (Binding d = this.getBinding(name); d != null; d = d.pop()) {
			X x = d.getHandled(c);
			if (x != null) {
				return f.match(x, c);
			}
		}
		return null;
	}

	public <X> X getLocal(String name, Class<X> c) {
		return this.getLocal(name, c, (d, c2) -> d);
	}

	public <X, Y> Y get(String name, Class<X> c, EnvMatcher<X, Y> f) {
		for (TEnv env = this; env != null; env = env.getParent()) {
			Y y = env.getLocal(name, c, f);
			if (y != null) {
				return y;
			}
		}
		return null;
	}

	public <X> X get(String name, Class<X> c) {
		return this.get(name, c, (d, c2) -> d);
	}

	public <X> X get(Class<X> c) {
		return this.get(c.getName(), c);
	}

	public <X> void findList(String name, Class<X> c, List<X> l, Predicate<X> f) {
		for (TEnv env = this; env != null; env = env.getParent()) {
			for (Binding d = env.getBinding(name); d != null; d = d.pop()) {
				X x = d.getHandled(c);
				if (x != null && f.test(x)) {
					l.add(x);
				}
			}
		}
	}

	public <X> void findList(Class<?> cname, Class<X> c, List<X> l, Predicate<X> f) {
		this.findList(this.key(cname), c, l, f);
	}

	public Code catchCode(Supplier<Code> f) {
		try {
			return f.get();
		} catch (ErrorCode e) {
			return e;
		}
	}

	public TLog setLogger() {
		TLog logs = new TLog();
		this.add(TLog.class, logs);
		return logs;
	}

	public TLog getLogger() {
		return this.get(TLog.class);
	}

	public void reportLog(TLog log) {
		TLog logger = this.getLogger();
		if (logger == null) {
			log.emit(TLog.Warning, TLog::report);
		} else {
			logger.append(log);
			// logger.emit(TLog.Warning, TLog::report);
		}
	}

	public void reportError(Code code, OFormat format, Object... args) {
		this.reportLog(new TLog(code.getSource(), TLog.Error, format, args));
	}

	public void reportError(Token s, OFormat format, Object... args) {
		this.reportLog(new TLog(s, TLog.Error, format, args));
	}

	public void reportWarning(Token s, OFormat format, Object... args) {
		this.reportLog(new TLog(s, TLog.Warning, format, args));
	}

	public void reportNotice(Token s, OFormat format, Object... args) {
		this.reportLog(new TLog(s, TLog.Notice, format, args));
	}

	public TEnv newEnv() {
		return new TEnv(this);
	}

	// public FuncEnv newFuncEnv() { // TopLevel
	// return new FuncEnv((Env) this, null, "", OArrays.emptyTokens,
	// OArrays.emptyTypes, Ty.tVarParam[0]);
	// }
	//
	// public FuncEnv newFuncEnv(String name, Token[] paramNames, Ty[] paramTypes,
	// Ty returnType) {
	// return new FuncEnv((Env) this, null, name, paramNames, paramTypes,
	// returnType);
	// }
	//
	// public FuncEnv newLambdaEnv(FuncCode fc) {
	// return new FuncEnv((Env) this, this.getFuncEnv(), "", fc.getParamNames(),
	// fc.getParamTypes(),
	// fc.getReturnType());
	// }

	/* class */

	public static class Binding implements Handled<Object> {
		private Object value;
		private Binding onstack = null;

		Binding(TEnv env, Token s, String name, Object value, Binding prev) {
			this.value = value;
			this.onstack = prev;
		}

		public Binding push(Binding onstack) {
			this.onstack = onstack;
			return this;
		}

		public Binding pop() {
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

	@FunctionalInterface
	public interface EnvMatcher<X, Y> {
		public Y match(X x, Class<X> c);
	}

	/* Type */
	public void setType(Class<?> c, TType t) {
		this.addBinding(null, c.getSimpleName(), t);
	}

	public TType getType(Class<?> c) {
		TType ty = this.get(c.getSimpleName(), TType.class);
		if (ty == null) {
			ty = new TClassType(c);
			this.setType(c, ty);
		}
		return ty;
	}

}

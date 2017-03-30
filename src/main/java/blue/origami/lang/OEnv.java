/***********************************************************************
 * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
 *  *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***********************************************************************/

package blue.origami.lang;

import java.util.HashMap;
import java.util.List;

import blue.origami.asm.OClassLoader;
import blue.origami.lang.type.OType;
import blue.origami.lang.type.OTypeSystem;
import blue.origami.nez.ast.SourcePosition;
import blue.origami.ocode.OCode;
import blue.origami.util.Handled;
import blue.origami.util.OStackable;
import blue.origami.util.OTypeUtils;

public interface OEnv {

	// public default void dump() {
	// for (OEnv e = this; e != null; e = e.getParent()) {
	// System.out.print(e.getName());
	// }
	// System.out.println();
	// }

	public default OEnv env() {
		return this;
	}

	public String getName();

	public default boolean isRuntime() {
		return false;
	}

	public Class<?> getEntryPoint();

	public default Class<?> findEntryPoint() {
		for (OEnv cur = this; cur != null; cur = cur.getParent()) {
			if (cur.getEntryPoint() != null) {
				return cur.getEntryPoint();
			}
		}
		return null;
	}

	public default OEnv getStartPoint() {
		return this; // FIXME
	}

	public static OEnv resolve(Class<?> entry) {
		OEnv env = (OEnv) OTypeUtils.loadFieldValue(entry, "entry");
		assert (env != null);
		return env;
	}

	public default OEnv newEnv() {
		return new OLocalEnv(this);
	}

	public default OEnv newEnv(String name) {
		return new OBaseEnv(this, name);
	}

	public OClassLoader getClassLoader();

	public OTypeSystem getTypeSystem();

	public default OType t(Class<?> c) {
		return getTypeSystem().newType(c);
	}

	public default OCode v(Object value) {
		return getTypeSystem().newValueCode(value);
	}

	public void addDefined(String name, OEnvEntry d);

	public default void hookDefined(String name, OEnvEntry d) {

	}

	public default void add(SourcePosition s, String name, Object value) {
		addDefined(name, new OEnvEntry(s, value));
	}

	public default void add(String name, Object value) {
		add(SourcePosition.UnknownPosition, name, value);
	}

	default String key(Class<?> c) {
		return c.getName();
	}

	public default void add(Class<?> cname, Object value) {
		add(key(cname), value);
	}

	public default <X> void set(String name, Class<X> c, X value) {
		for (OEnvEntry d = this.getDefined(name, this.isRuntime()); d != null; d = d.pop()) {
			X x = d.getHandled(c);
			if (x != null) {
				d.setHandled(value);
				return;
			}
		}
		add(name, value);
	}

	public OEnvEntry getDefined(String name, boolean isRuntime);

	public default <X, Y> Y getLocal(String name, boolean isRuntime, Class<X> c, OEnvMatcher<X, Y> f) {
		for (OEnvEntry d = this.getDefined(name, isRuntime); d != null; d = d.pop()) {
			X x = d.getHandled(c);
			if (x != null) {
				return f.match(x, c);
			}
		}
		return null;
	}

	public default <X> X getLocal(String name, boolean isRuntime, Class<X> c) {
		return this.getLocal(name, isRuntime, c, (d, c2) -> d);
	}

	public OEnv getParent();

	public default <X, Y> Y get(String name, Class<X> c, OEnvMatcher<X, Y> f) {
		for (OEnv env = this; env != null; env = env.getParent()) {
			Y y = env.getLocal(name, this.isRuntime(), c, f);
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

	public interface OEnvChoicer<X> {
		public X choice(X x, X y);
	}

	public default <X, Y> Y find(String name, Class<X> c, OEnvMatcher<X, Y> f, OEnvChoicer<Y> g, Y start) {
		Y y = start;
		for (OEnv env = this; env != null; env = env.getParent()) {
			for (OEnvEntry d = env.getDefined(name, this.isRuntime()); d != null; d = d.pop()) {
				X x = d.getHandled(c);
				if (x != null) {
					Y updated = f.match(x, c);
					y = g.choice(y, updated);
				}
			}
		}
		return y;
	}

	public interface OEnvBreaker<X> {
		public boolean isEnd(X x);
	}

	public default <X, Y> Y find(String name, Class<X> c, OEnvMatcher<X, Y> f, OEnvChoicer<Y> g, Y start,
			OEnvBreaker<Y> h) {
		Y y = start;
		for (OEnv env = this; env != null; env = env.getParent()) {
			for (OEnvEntry d = env.getDefined(name, this.isRuntime()); d != null; d = d.pop()) {
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
		for (OEnv env = this; env != null; env = env.getParent()) {
			for (OEnvEntry d = env.getDefined(name, this.isRuntime()); d != null; d = d.pop()) {
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

	@FunctionalInterface
	public static interface OEnvMatcher<X, Y> {
		public Y match(X x, Class<X> c);
	}

	/* Class */

	public static class OBaseEnv extends OMapEnv implements OEnv {
		final OEnv parent_;
		Class<?> entryPoint = null;
		private final String name;
		private final OTypeSystem typeSystem;

		protected OBaseEnv(OTypeSystem ts) {
			this.parent_ = null;
			this.name = "__root__";
			this.typeSystem = ts;
		}

		protected OBaseEnv(OEnv parent, String name) {
			this.parent_ = parent;
			this.name = name;
			this.typeSystem = parent.getTypeSystem();
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public Class<?> getEntryPoint() {
			if (this.entryPoint == null) {
				this.entryPoint = this.getClassLoader().entryPoint(this, new RuntimeEnv(this));
			}
			return this.entryPoint;
		}

		@Override
		public OEnv getParent() {
			return this.parent_;
		}

		@Override
		public OClassLoader getClassLoader() {
			return this.typeSystem.getClassLoader();
		}

		@Override
		public OTypeSystem getTypeSystem() {
			return this.typeSystem;
		}
	}

	public static class OLocalEnv extends OMapEnv implements OEnv {
		final OEnv parent;

		protected OLocalEnv(OEnv parent) {
			assert (parent != this);
			this.parent = parent;
		}

		@Override
		public String getName() {
			return this.getParent() + "+";
		}

		@Override
		public Class<?> getEntryPoint() {
			return null;
		}

		@Override
		public OEnv getParent() {
			return this.parent;
		}

		@Override
		public OClassLoader getClassLoader() {
			return this.getParent().getClassLoader();
		}

		@Override
		public OTypeSystem getTypeSystem() {
			return this.getParent().getTypeSystem();
		}

	}

	static class RuntimeEnv extends OLocalEnv implements OEnv {

		RuntimeEnv(OEnv parent) {
			super(parent);
		}

		@Override
		public boolean isRuntime() {
			return true;
		}

	}

	public static class OEnvEntry implements OStackable<OEnvEntry>, Handled<Object> {
		private Object value;
		private OEnvEntry onstack = null;

		OEnvEntry(SourcePosition s, Object value) {
			this.value = value;
		}

		@Override
		public OEnvEntry push(OEnvEntry onstack) {
			this.onstack = onstack;
			return this;
		}

		@Override
		public OEnvEntry pop() {
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

}

abstract class OMapEnv implements OEnv {
	HashMap<String, OEnvEntry> definedMap = null;

	// final void setDefined(String name, Defined2 defined) {
	// if (definedMap == null) {
	// definedMap = new HashMap<>();
	// }
	// this.definedMap.put(name, defined);
	// }

	@Override
	public OEnvEntry getDefined(String name, boolean isRuntime) {
		if (this.definedMap != null) {
			return this.definedMap.get(name);
		}
		return null;
	}

	@Override
	public void addDefined(String name, OEnvEntry defined) {
		if (this.definedMap == null) {
			this.definedMap = new HashMap<>();
		}
		OEnvEntry prev = this.definedMap.get(name);
		defined.push(prev);
		this.definedMap.put(name, defined);
		// ODebug.trace("adding symbol %s %s on %s at env %s", name,
		// defined.getHandled(), defined.pop(),
		// this.getClass().getSimpleName());
		this.hookDefined(name, defined);
	}

	@Override
	public String toString() {
		return this.getName();
	}

}

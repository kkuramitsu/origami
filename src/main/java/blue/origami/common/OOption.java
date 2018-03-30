/***********************************************************************
 * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
 * 
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

package blue.origami.common;

import java.lang.reflect.Array;
import java.util.HashMap;

import blue.origami.Version;
import origami.nez2.OStrings;

public class OOption {
	private HashMap<String, Object> valueMap = new HashMap<>();

	public interface OOptionKey {
		public OOptionKey keyOf(String key);
	}

	private String tokey(OOptionKey key) {
		return key.toString();
	}

	public void set(OOptionKey key, Object value) {
		this.valueMap.put(this.tokey(key), value);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(OOptionKey key) {
		return (T) this.valueMap.get(this.tokey(key));
	}

	public <T> void add(OOptionKey key, T[] list) {
		T[] olist = this.get(key);
		if (olist == null) {
			this.set(key, list);
		} else {
			@SuppressWarnings("unchecked")
			T[] l = (T[]) Array.newInstance(list.getClass().getComponentType(), olist.length + list.length);
			System.arraycopy(olist, 0, l, 0, olist.length);
			System.arraycopy(list, 0, l, olist.length, list.length);
			this.set(key, l);
		}
	}

	public OOptionKey checkKeyName(String t, OOptionKey... keys) {
		for (OOptionKey k : keys) {
			OOptionKey r = k.keyOf(t);
			if (r != null) {
				return r;
			}
		}
		return null;
	}

	public final void setKeyValue(String option, OOptionKey... keys) {
		int loc = option.indexOf('=');
		if (loc > 0) {
			OOptionKey name = this.checkKeyName(option.substring(0, loc), keys);
			if (name == null) {
				return;
			}
			String value = option.substring(loc + 1);
			if (value.equals("true")) {
				this.set(name, true);
				return;
			}
			if (value.equals("false")) {
				this.set(name, false);
				return;
			}
			try {
				int nvalue = Integer.parseInt(value);
				this.set(name, nvalue);
				return;
			} catch (Exception e) {
			}
			try {
				double nvalue = Double.parseDouble(value);
				this.set(name, nvalue);
				return;
			} catch (Exception e) {
			}
			this.set(name, value);
			return;
		}
	}

	public final boolean is(OOptionKey key, boolean defval) {
		Boolean b = this.get(key);
		return b == null ? defval : b;
	}

	public final int intValue(OOptionKey key, int defval) {
		Integer b = this.get(key);
		return b == null ? defval : b;
	}

	public final String stringValue(OOptionKey key, String defval) {
		String b = this.get(key);
		return b == null ? defval : b;
	}

	public final String[] stringList(OOptionKey key) {
		Object o = this.get(key);
		if (o instanceof String[]) {
			return (String[]) o;
		}
		if (o != null) {
			return new String[] { o.toString() };
		}
		return new String[0];
	}

	HashMap<Class<?>, Object> classMap = new HashMap<>();

	// -X class
	public void setClass(String className) throws Throwable {
		Class<?> c = loadClass(OFactory.class, className, Version.ClassPath + ".parser.nezcc",
				Version.ClassPath + ".transpiler.target");
		OFactory<?> f = (OFactory<?>) c.newInstance();
		this.classMap.put(f.keyClass(), f);
	}

	@SuppressWarnings("unchecked")
	private <T> T get(Class<T> c) {
		return (T) this.classMap.get(c);
	}

	public <T extends OFactory<T>> T newInstance(Class<? extends T> c) {
		T value = this.get(c);
		if (value != null) {
			value = value.clone();
		} else {
			try {
				value = c.newInstance();
			} catch (Exception e) {
				OConsole.exit(1, e);
			}
		}
		value.init(this);
		return value;
	}

	public static Class<?> loadClass(Class<?> base, String className, String... path) throws ClassNotFoundException {
		if (path != null) {
			for (String cpath : path) {
				String cname = cpath + "." + className;
				try {
					Class<?> c = Class.forName(cname);
					if (base.isAssignableFrom(c)) {
						return c;
					}
				} catch (ClassNotFoundException e) {
				}
			}
		}
		return Class.forName(className);
	}

	// Logging

	static enum VerboseOption implements OOptionKey {
		SourceLogger;

		@Override
		public String toString() {
			return this.name();
		}

		@Override
		public OOptionKey keyOf(String key) {
			return VerboseOption.valueOf(key);
		}
	}

	private SourceLogger logger() {
		SourceLogger logger = this.get(VerboseOption.SourceLogger);
		if (logger == null) {
			logger = new SourceLogger.SimpleSourceLogger();
			this.set(VerboseOption.SourceLogger, logger);
		}
		return logger;
	}

	public final void reportError(SourcePosition s, OFormat fmt, Object... args) {
		this.logger().reportError(s, fmt, args);
	}

	public final void reportWarning(SourcePosition s, OFormat fmt, Object... args) {
		this.logger().reportWarning(s, fmt, args);
	}

	public final void reportNotice(SourcePosition s, OFormat fmt, Object... args) {
		this.logger().reportNotice(s, fmt, args);
	}

	// Messaging

	private boolean verboseMode = false;

	public final void setVerbose(boolean b) {
		this.verboseMode = b;
	}

	public final void verbose(String fmt, Object... a) {
		if (this.verboseMode) {
			OConsole.beginColor(34);
			OConsole.println(OStrings.format(fmt, a));
			OConsole.endColor();
		}
	}

	public final long nanoTime(String msg, long t1) {
		long t2 = System.nanoTime();
		if (this.verboseMode && msg != null) {
			double d = (t2 - t1) / 1000000;
			if (d > 0.1) {
				this.verbose("%s : %.2f[ms]", msg, d);
			}
		}
		return t2;
	}
}

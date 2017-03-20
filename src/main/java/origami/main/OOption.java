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

package origami.main;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.TreeMap;

import origami.OConsole;
import origami.nez.ast.SourceLogger;
import origami.nez.ast.SourcePosition;
import origami.rule.LocaleFormat;
import origami.trait.StringCombinator;

public class OOption {
	private HashMap<String, Object> valueMap = new HashMap<>();
	
	public interface Key {
		public Key keyOf(String key);
	}
	
	private String tokey(Key key) {
		return key.toString();
	}
	
	public void set(Key key, Object value) {
		valueMap.put(tokey(key), value);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(Key key) {
		return (T) valueMap.get(tokey(key));
	}

	public <T> void add(Key key, T[] list) {
		T[] olist = get(key);
		if(olist == null) {
			set(key, list);
		}
		else {
			@SuppressWarnings("unchecked")
			T[] l = (T[])Array.newInstance(list.getClass().getComponentType(), olist.length + list.length);
			System.arraycopy(olist, 0, l, 0, olist.length);
			System.arraycopy(list, 0, l, olist.length, list.length);
			set(key, l);
		}
	}

	public Key checkKeyName(String t, Key ... keys) {
		for(Key k : keys) {
			Key r = k.keyOf(t);
			if(r != null) {
				return r;
			}
		}
		return null;
	}
	
	
	public final void setKeyValue(String option, Key ...keys) {
		int loc = option.indexOf('=');
		if (loc > 0) {
			Key name = checkKeyName(option.substring(0, loc), keys);
			if(name == null) {
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
				set(name, nvalue);
				return;
			} catch (Exception e) {
			}
			try {
				double nvalue = Double.parseDouble(value);
				set(name, nvalue);
				return;
			} catch (Exception e) {
			}
			this.set(name, value);
			return;
		}
	}

	public final boolean is(Key key, boolean defval) {
		Boolean b = get(key);
		return b == null ? defval : b;
	}

	public final int intValue(Key key, int defval) {
		Integer b = get(key);
		return b == null ? defval : b;
	}

	public final String value(Key key, String defval) {
		String b = get(key);
		return b == null ? defval : b;
	}

	public final String[] list(Key key) {
		Object o = get(key);
		if (o instanceof String[]) {
			return (String[]) o;
		}
		if (o != null) {
			return new String[]{o.toString()};
		}
		return new String[0];
	}
	
	HashMap<Class<?>, Object> classMap = new HashMap<>();

	public <T> void set(Class<T> c, T value) {
		classMap.put(c, value);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> c) {
		return (T) classMap.get(c);
	}

	public interface OptionalFactory<T> extends Cloneable {
		public Class<?> entryClass();
		public T clone();
		public void init(OOption options);
	}

	public <T extends OptionalFactory<T>> T newInstance(Class<? extends T> c) {
		T value = get(c);
		if(value != null) {
			value = value.clone();		
		}
		else {
			try {
				value = c.newInstance();
			}
			catch(Exception e) {
				OConsole.exit(1, e);
			}
		}
		value.init(this);
		return value;
	}

	public void setClass(String path) throws Throwable {
		Class<?> c = Class.forName(path);
		OptionalFactory<?> f = (OptionalFactory<?>) c.newInstance();
		classMap.put(f.entryClass(), f);
	}

	public Class<?> loadClass(String className, String path[]) throws ClassNotFoundException {
		if(path != null) {
			for(String cpath: path) {
				String cname = cpath + "." + className;
				try {
					return Class.forName(cname);
				}
				catch(ClassNotFoundException e) {
				}
			}
		}
		return Class.forName(className);
	}
	
	// Logging
	
	static enum VerboseOption implements Key {
		SourceLogger;

		public String toString() {
			return name();
		}

		@Override
		public Key keyOf(String key) {
			return VerboseOption.valueOf(key);
		}
	}
	
	public final void reportError(SourcePosition s, String fmt, Object... args) {
//		if (error() >= 1) {
//			report(Error, message(s, "error", fmt, args));
//		}
	}

	public final void reportWarning(SourcePosition s, String fmt, Object... args) {
//		if (error() >= 2) {
//			report(Warning, message(s, "warning", fmt, args));
//		}
	}

	public final void reportNotice(SourcePosition s, String fmt, Object... args) {
//		if (error() >= 3) {
//			report(Notice, message(s, "notice", fmt, args));
//		}
	}

	private SourceLogger log() {
		SourceLogger log = this.get(VerboseOption.SourceLogger);
		if(log == null) {
			log = new SourceLogger.SimpleSourceLogger();
			this.set(VerboseOption.SourceLogger, log);
		}
		return log;
	}
	public final void reportError(SourcePosition s, LocaleFormat fmt, Object... args) {
		log().reportError(s, fmt, args);
	}

	public final void reportWarning(SourcePosition s, LocaleFormat fmt, Object... args) {
		log().reportError(s, fmt, args);
	}

	public final void reportNotice(SourcePosition s, LocaleFormat fmt, Object... args) {
		log().reportError(s, fmt, args);
	}
	
	// Messaging
	
	private boolean verboseMode = false;
	public final void verbose(String fmt, Object... a) {
		if (verboseMode) {
			OConsole.beginColor(34);
			OConsole.println(StringCombinator.format(fmt, a));
			OConsole.endColor();
		}
	}

	public final long nanoTime(String msg, long t1) {
		long t2 = System.nanoTime();
		if (verboseMode && msg != null) {
			double d = (t2 - t1) / 1000000;
			if (d > 0.1) {
				verbose("%s : %.2f[ms]", msg, d);
			}
		}
		return t2;
	}


}

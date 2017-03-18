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

package origami.nez.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import origami.OConsole;
import origami.nez.ast.CommonTree;
import origami.nez.ast.Source;
import origami.nez.ast.SourcePosition;
import origami.nez.ast.Tree;
import origami.nez.parser.ParserCode.StaticMemoization;
import origami.nez.parser.pass.DispatchPass;
import origami.nez.parser.pass.InlinePass;
import origami.nez.parser.pass.NotCharPass;
import origami.nez.parser.pass.TreePass;
import origami.nez.peg.ParserCombinator;
import origami.nez.peg.Grammar;
import origami.nez.peg.Production;

public class ParserFactory {

	static HashMap<String, String> keyMap = new HashMap<>();

	static void initKey(String key) {
		keyMap.put(key, key);
	}

	static void initKey(String key, String alias) {
		keyMap.put(key, alias);
	}

	static {
		initKey("tree");
	}

	public final static boolean isDefined(String key) {
		return keyMap.containsKey(key);
	}

	private TreeMap<String, Object> valueMap;
	private Grammar defaultGrammar;
	private Compiler optionalCompiler;
	private StaticMemoization optionalStaticMemoization;
	private ParserContext<?> optionalContext;
	private GrammarWriter defaultGrammarWriter;

	private Tree<?> defaultTree;
	private TreeWriter defaultTreeWriter;
	private boolean verboseMode;

	@SuppressWarnings("unchecked")
	private ParserFactory(ParserFactory fac, Grammar g) {
		if (fac == null) {
			this.verboseMode = false; // FIXME
			this.valueMap = new TreeMap<>();
			this.defaultGrammar = g;
			this.optionalCompiler = null;
			this.optionalStaticMemoization = null;
			this.optionalContext = null;
			this.defaultTree = null;
			this.defaultTreeWriter = null;
			this.defaultGrammarWriter = null;
			this.trapList = new ArrayList<>();
		} else {
			this.valueMap = (TreeMap<String, Object>) fac.valueMap.clone();
			this.defaultGrammar = g == null ? fac.defaultGrammar : g;
			this.optionalCompiler = fac.optionalCompiler;
			this.optionalStaticMemoization = fac.optionalStaticMemoization;
			this.optionalContext = fac.optionalContext;
			this.defaultGrammarWriter = fac.defaultGrammarWriter;
			this.defaultTree = fac.defaultTree;
			this.defaultTreeWriter = fac.defaultTreeWriter;
			this.trapList = fac.trapList;
		}
	}

	public ParserFactory() {
		this(null, null);
	}

	public ParserFactory newFactory(Grammar g) {
		return new ParserFactory(this, g);
	}

	private String key(String key) {
		String k = keyMap.get(key);
		return k == null ? key : k;
	}

	public void set(String key, Object value) {
		valueMap.put(key(key), value);
	}

	public void add(String key, String value) {
		key = key(key);
		@SuppressWarnings("unchecked") List<String> l = (List<String>) valueMap.get(key);
		if (l == null) {
			l = new ArrayList<>(8);
			valueMap.put(key, l);
		}
		l.add(value);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		return (T) valueMap.get(key(key));
	}

	public final boolean is(String key, boolean defval) {
		Boolean b = get(key);
		return b == null ? defval : b;
	}

	public final int intValue(String key, int defval) {
		Integer b = get(key);
		return b == null ? defval : b;
	}

	public final String value(String key, String defval) {
		String b = get(key);
		return b == null ? defval : b;
	}

	public final String[] list(String key) {
		Object o = get(key);
		if (o == null) {
			return new String[0];
		}
		if (o instanceof String[]) {
			return (String[]) o;
		}
		@SuppressWarnings("unchecked") List<String> l = (List<String>) o;
		return l.toArray(new String[l.size()]);
	}

	public final Source newSource(String path) throws IOException {
		return CommonSource.newFileSource(path, list("path"));
	}

	public final boolean TreeOption() {
		return this.is("tree", true);
	}

	public final boolean MemoOption() {
		return this.is("memo", true);
	}

	public boolean DetreeOption() {
		return true; // allowing detree instruction
	}

	public final void setOption(String option) {
		int loc = option.indexOf('=');
		if (loc > 0) {
			String name = option.substring(0, loc);
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
		if (option.startsWith("-")) {
			this.set(option.substring(1), false);
			return;
		}
		if (option.startsWith("+")) {
			this.set(option.substring(1), true);
			return;
		}
	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		int c = 0;
		for (String key : this.valueMap.keySet()) {
			if (c > 0) {
				sb.append(",");
			}
			sb.append(key);
			sb.append("=");
			sb.append(valueMap.get(key));
			c++;
		}
		sb.append("]");
		return sb.toString();
	}

	public boolean loadClass(String path) throws IOException {
		try {
			Object x = Class.forName(path).newInstance();
			if (x instanceof TreeWriter) {
				defaultTreeWriter = (TreeWriter) x;
				return true;
			}
			if (x instanceof GrammarWriter) {
				defaultGrammarWriter = (GrammarWriter) x;
				return true;
			}
			if (x instanceof Grammar) {
				defaultGrammar = (Grammar) x;
				return true;
			}
			if (x instanceof ParserCombinator) {
				defaultGrammar = new Grammar();
				((ParserCombinator) x).load(this, defaultGrammar, value("start", null));
				return true;
			}
			if (x instanceof Compiler) {
				optionalCompiler = (Compiler) x;
				return true;
			}
			if (x instanceof NZ86ParserContext<?>) {
				optionalContext = (NZ86ParserContext<?>) x;
				return true;
			}
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new ParserException("undefined class path: " + path + " by " + e);
		}
		return false;
	}

	private <T> T newInstance(Class<T> c) {
		try {
			return c.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			this.trace(e);
		}
		return null;
	}

	/* -------------------------------------------------------------------- */

	public static class ParserException extends IOException {
		/**
		 * 
		 */
		private static final long serialVersionUID = -8529797719822194498L;

		public ParserException(String msg) {
			super(msg);
		}
	}

	public final Grammar getGrammar() throws IOException {
		if (defaultGrammar == null) {
			String path = value("grammar", null);
			if (path != null) {
				defaultGrammar = Grammar.loadFile(path, list("grammar-path"));
			}
			if (defaultGrammar == null) {
				defaultGrammar = new Grammar(); // Empty
			}
		}
		return defaultGrammar;
	}

	public final Parser newParser() throws IOException {
		return this.newParser(null);
	}

	public final Parser newParser(String start) throws IOException {
		Grammar g = this.getGrammar();
		if (start == null) {
			start = value("start", null);
		}
		if (start != null) {
			Production p = g.getProduction(start);
			if (p != null) {
				return new Parser(this, p);
			}
			return null;
		}
		return new Parser(this, g.getStartProduction());
	}

	public interface Compiler {
		public Executable compile(ParserFactory fac, Grammar grammar);
	}

	public interface Executable {
		public Grammar getGrammar();

		public <T> void initContext(ParserContext<T> ctx);

		public <T> T exec(ParserContext<T> ctx);

	}

	public Grammar optimize(ParserFactory fac, Production start) {
		return new GrammarChecker(fac, start).checkGrammar();
	}

	public void applyPass(Grammar g) {
		if (this.is("raw", false)) {
			return;
		}
		String[] pass = this.list("pass");
		if (pass.length > 0) {
			Pass.apply(this, g, pass);
		} else {
			Pass.apply(this, g, NotCharPass.class, TreePass.class, DispatchPass.class, InlinePass.class);
		}
	}

	public Compiler newCompiler() {
		if (optionalCompiler == null) {
			return new NZ86Compiler();
		}
		return optionalCompiler;
	}

	public StaticMemoization newStaticMemoization() {
		if (optionalStaticMemoization != null) {
			return this.optionalStaticMemoization;
		}
		return new ParserCode.StaticMemoization();
	}

	public final <T> ParserContext<T> newContext(Source s, TreeConstructor<T> newTree, TreeConnector<T> linkTree) {
		if (optionalContext == null) {
			NZ86ParserContext<T> c = new NZ86ParserContext<>(s, newTree, linkTree);
			c.setTrap(this.trapList.toArray(new TrapAction[this.trapList.size()]));
			return c;
		}
		return optionalContext.newInstance(s, newTree, linkTree);
	}

	public final Tree<?> newTree() {
		if (defaultTree != null) {
			return defaultTree;
		}
		return new CommonTree();
	}

	public interface GrammarWriter {
		public void setPath(String path);

		public void writeGrammar(ParserFactory fac, Grammar grammar);

		public void close();
	}

	public final <T extends GrammarWriter> GrammarWriter newGrammarWriter(Class<T> c) {
		return defaultGrammarWriter == null ? (c != null ? newInstance(c) : null) : defaultGrammarWriter;
	}

	public interface TreeWriter {
		public void setPath(String path);

		public void writeTree(ParserFactory fac, Tree<?> node);

		public void close();
	}

	public final <T extends TreeWriter> TreeWriter newTreeWriter(Class<T> c) {
		return defaultTreeWriter == null ? (c != null ? newInstance(c) : null) : defaultTreeWriter;
	}

	// ----------------------------------------------------------------------
	// trap

	private List<TrapAction> trapList;

	public final int addTrapAction(TrapAction a) {
		for (int i = 0; i < trapList.size(); i++) {
			if (trapList.get(i).getClass() == a.getClass()) {
				return i;
			}
		}
		trapList.add(a);
		return trapList.size() - 1;
	}

	// // ----------------------------------------------------------------------
	// // reporter

	public final static int Error = 31;
	public final static int Warning = 35;
	public final static int Notice = 36;
	public final static int Info = 37;

	void report(int level, String msg) {
		OConsole.beginColor(level);
		OConsole.println(msg);
		OConsole.endColor();
	}

	private int error() {
		return intValue("error", 3);
	}

//	private String message(SourcePosition s, String type, String fmt, Object... args) {
//		if (s != null) {
//			return s.formatSourceMessage(type, String.format(fmt, args));
//		}
//		return "(" + type + ") " + String.format(fmt, args);
//	}

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

	public final void reportInfo(SourcePosition s, String fmt, Object... args) {
//		if (error() >= 4) {
//			report(Info, message(s, "info", fmt, args));
//		}
	}

	// -------------------------------------------------------------------------

	public void setVerboseMode(boolean b) {
		this.verboseMode = b;
	}

	public final void verbose(String fmt, Object... a) {
		if (verboseMode) {
			OConsole.beginColor(34);
			OConsole.println(String.format(fmt, a));
			OConsole.endColor();
		}
	}

	public final void trace(Throwable e) {
		if (verboseMode) {
			OConsole.beginColor(34);
			e.printStackTrace(System.out);
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

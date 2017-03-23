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

package origami;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import origami.code.OCode;
import origami.code.OCodeWriter;
import origami.code.OErrorCode;
import origami.lang.OEnv;
import origami.lang.type.OTypeSystem;
import origami.main.ParserOption;
import origami.nez.ast.Source;
import origami.nez.ast.SourcePosition;
import origami.nez.ast.Tree;
import origami.nez.parser.Parser;
import origami.nez.parser.ParserSource;
import origami.nez.peg.Grammar;
import origami.nez.peg.Production;
import origami.rule.OrigamiTypeSystem;
import origami.rule.TypeAnalysis;
import origami.util.OConsole;
import origami.util.ODebug;
import origami.util.OOption;
import origami.util.OScriptUtils;
import origami.util.OTree;

public class OrigamiContext extends OEnv.OBaseEnv {
	final OOption options;
	final OrigamiRuntime runtime = new OrigamiRuntime();

	OrigamiContext(OTypeSystem ts, OOption options) {
		super(ts == null ? options.newInstance(OrigamiTypeSystem.class) : ts);
		this.getTypeSystem().init(this, SourcePosition.UnknownPosition);
		this.options = options;
	}

	public OrigamiContext() {
		this((OTypeSystem) null);
	}

	public OrigamiContext(OTypeSystem ts) {
		this(ts, new OOption());
	}

	public OrigamiContext(Grammar grammar) throws ClassNotFoundException {
		this(grammar, new OOption());
	}

	public OrigamiContext(Grammar grammar, OOption options) throws ClassNotFoundException {
		this(loadTypeSystem(grammar), options);
		this.setGrammar(grammar);
	}

	static OTypeSystem loadTypeSystem(Grammar g) throws ClassNotFoundException {
		Production pp = g.getProduction("ORIGAMI");
		if (pp != null) {
			String cpath = pp.getExpression().toString().replaceAll("'", "");
			// ODebug.trace("ORIGAMI=%s", cpath);
			try {
				return (OTypeSystem) Class.forName(cpath).newInstance();
			} catch (ClassNotFoundException e) {
				throw e;
			} catch (Exception e) {
				OConsole.exit(1, e);
			}
		}
		return null;
	}

	public OrigamiContext(OOption options) throws IOException, ClassNotFoundException {
		this(loadGrammar(options), options);
	}

	static Grammar loadGrammar(OOption options) throws IOException {
		String file = options.value(ParserOption.GrammarFile, "iroha.nez");
		return Grammar.loadFile(file, options.list(ParserOption.GrammarPath));
	}

	public void setGrammar(Grammar grammar) {
		Parser p = grammar.newParser();
		this.add(Parser.class, p);
		this.add(Grammar.class, grammar);
	}

	public void importClass(Class<?> c) throws IOException {
		this.runtime.importClass(this, null, c, null);
	}

	public boolean loadScriptFile(String path) throws IOException {
		return this.loadScriptFile(ParserSource.newFileSource(path, null));
	}

	public boolean loadScriptFile(Source sc) {
		try {
			this.runtime.load(this, sc);
			return true;
		} catch (Throwable e) {
			this.showThrowable(e);
			return false;
		}
	}

	public void testScriptFile(Source sc) throws Throwable {
		try {
			this.runtime.load(this, sc);
		} catch (Throwable e) {
			this.showThrowable(e);
			throw e;
		}
	}

	void showThrowable(Throwable e) {
		if (e instanceof InvocationTargetException) {
			this.showThrowable(((InvocationTargetException) e).getTargetException());
			return;
		}
		if (e instanceof OErrorCode) {
			OConsole.println(OConsole.bold("Static Error: "));
			OConsole.beginColor(OConsole.Red);
			OConsole.println(((OErrorCode) e).getLog());
			OConsole.endColor();
		} else {
			OConsole.println(OConsole.bold("Runtime Exception: "));
			OConsole.beginColor(OConsole.Yellow);
			e.printStackTrace();
			OConsole.endColor();
		}
	}

	public Object eval(String source, int line, String script) throws Throwable {
		Source sc = ParserSource.newStringSource(source, line, script);
		return this.runtime.eval(this, sc);
	}

	public void shell(String source, int line, String script) {
		Source sc = ParserSource.newStringSource(source, line, script);
		this.runtime.runREPL(this, sc);
	}

	class OrigamiRuntime extends OConsole implements OScriptUtils, TypeAnalysis {
		OTree defaultTree = new OTree();
		OCodeWriter eval = new OCodeWriter();

		public void load(OEnv env, Source sc) throws Throwable {
			Parser p = env.get(Parser.class);
			Tree<?> t = p.parse(sc, 0, this.defaultTree, this.defaultTree);
			OCode code = this.typeExpr(env, t);
			code.eval(env);
		}

		private Tree<?> parseTree(OEnv env, Source sc) throws IOException {
			Parser p = env.get(Parser.class);
			return p.parse(sc, 0, this.defaultTree, this.defaultTree);
		}

		public Object eval(OEnv env, Source sc) throws Throwable {
			Tree<?> node = this.parseTree(env, sc);
			OCode code = this.typeExpr(env, node);
			return code.eval(env);
		}

		public boolean runREPL(OEnv env, Source sc) {
			try {
				Tree<?> node = this.parseTree(env, sc);
				if (node == null) {
					return false;
				}
				if (ODebug.isDebug()) {
					beginColor(Blue);
					dump("  ", node.toString());
					endColor();
				}
				OCode code = this.typeExpr(env, node);
				// env.add(LocalVariables.class, new LocalVariables());
				OCodeWriter eval = OrigamiContext.this.options.newInstance(OCodeWriter.class);
				eval.writeln(env, code);
				// Object value = code.eval(env);
				// if (!code.getType().is(void.class)) {
				// String t2 = code.getType().toString();
				// StringBuilder sb = new StringBuilder();
				// sb.append(color(Gray, " => "));
				// StringCombinator.appendQuoted(sb, value);
				// beginColor(sb, Cyan);
				// sb.append(" :");
				// StringCombinator.append(sb, t2);
				// endColor(sb);
				// println(sb.toString());
				// }
				return true;
			} catch (Throwable e) {
				OrigamiContext.this.showThrowable(e);
			}
			return false;
		}

	}

	@SuppressWarnings("serial")
	public static class LocalVariables extends HashMap<String, Object> {

	}

}

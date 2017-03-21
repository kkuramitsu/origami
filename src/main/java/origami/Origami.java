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

import origami.code.OErrorCode;
import origami.code.OCode;
import origami.lang.callsite.OFuncCallSite;
import origami.lang.callsite.OGetterCallSite;
import origami.lang.callsite.OMethodCallSite;
import origami.nez.ast.Source;
import origami.nez.ast.Tree;
import origami.nez.parser.ParserSource;
import origami.nez.parser.Parser;
import origami.nez.peg.Grammar;
import origami.nez.peg.Production;
import origami.rule.TypeAnalysis;
import origami.trait.OScriptUtils;
import origami.trait.StringCombinator;

public class Origami extends OEnv.OBaseEnv {
	final OrigamiRuntime runtime = new OrigamiRuntime();

	public Origami(Grammar grammar) throws IOException {
		super(null, "__root__");
		add(OFuncCallSite.class, new OFuncCallSite());
		add(OMethodCallSite.class, new OMethodCallSite());
		add(OGetterCallSite.class, new OGetterCallSite());
		init(grammar);
		Parser p = grammar.newParser();
		this.add(Parser.class, p);
		this.add(Grammar.class, grammar);
	}

	public void init(Grammar g) throws IOException {
		Production pp = g.getProduction("ORIGAMI");
		if (pp != null) {
			String c = pp.getExpression().toString().replaceAll("'", "");
			// ODebug.trace("ORIGAMI=%s", c);
			try {
				importClass(Class.forName(c));
			} catch (ClassNotFoundException e) {
				ODebug.traceException(e);
				throw new IOException("cannot load " + c);
			}
		} else {
			ODebug.println("set ORIGAMI in nez file");
			importClass(origami.rule.iroha.IrohaSet.class);
		}
	}

	public void importClass(Class<?> c) throws IOException {
		runtime.importClass(this, null, c, null);
	}

	public boolean loadScriptFile(String path) throws IOException {
		Source sc = ParserSource.newFileSource(path, null);
		try {
			runtime.load(this, sc);
			return true;
		} catch (Throwable e) {
			showThrowable(e);
			return false;
		}
	}

	public boolean loadScriptFile(Source sc) throws IOException {
		try {
			runtime.load(this, sc);
			return true;
		} catch (Throwable e) {
			showThrowable(e);
			return false;
		}
	}

	public Object eval(String source, int line, String script) throws Throwable {
		Source sc = ParserSource.newStringSource(source, line, script);
		return runtime.eval(this, sc);
	}

	public void shell(String source, int line, String script) {
		Source sc = ParserSource.newStringSource(source, line, script);
		runtime.runREPL(this, sc);
	}

	class OrigamiRuntime extends OConsole implements OScriptUtils, TypeAnalysis {
		OTree defaultTree = new OTree();

		public void load(OEnv env, Source sc) throws Throwable {
			Parser p = env.get(Parser.class);
			//p.setThrowingException(true);
			//p.setPrintingException(true);
			Tree<?> t = p.parse(sc, 0, defaultTree, defaultTree);
			OCode code = typeExpr(env, t);
			code.eval(env);
		}

		private Tree<?> parseTree(OEnv env, Source sc) throws IOException {
			Parser p = env.get(Parser.class);
			//p.setThrowingException(false);
			//p.setPrintingException(true);
			return p.parse(sc, 0, defaultTree, defaultTree);
		}

		public Object eval(OEnv env, Source sc) throws Throwable {
			Tree<?> node = parseTree(env, sc);
			OCode code = typeExpr(env, node);
			return code.eval(env);
		}

		public boolean runREPL(OEnv env, Source sc) {
			try {
				Tree<?> node = parseTree(env, sc);
				if (node == null) {
					return false;
				}
				if (ODebug.isDebug()) {
					beginColor(Blue);
					dump("  ", node.toString());
					endColor();
				}
				OCode code = typeExpr(env, node);
				env.add(LocalVariables.class, new LocalVariables());
				Object value = code.eval(env);
				if (!code.getType().is(void.class)) {
					String t2 = code.getType().toString();
					StringBuilder sb = new StringBuilder();
					sb.append(color(Gray, " => "));
					StringCombinator.appendQuoted(sb, value);
					beginColor(sb, Cyan);
					sb.append(" :");
					StringCombinator.append(sb, t2);
					endColor(sb);
					println(sb.toString());
				}
				return true;
			} catch (Throwable e) {
				showThrowable(e);
			}
			return false;
		}

	}

	@SuppressWarnings("serial")
	public static class LocalVariables extends HashMap<String, Object> {

	}

	public static void showThrowable(Throwable e) {
		if (e instanceof InvocationTargetException) {
			showThrowable(((InvocationTargetException) e).getTargetException());
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

}

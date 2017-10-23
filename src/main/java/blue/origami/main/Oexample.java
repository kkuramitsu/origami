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

package blue.origami.main;

import java.io.IOException;
import java.util.HashMap;

import blue.origami.main.Otest.Coverage;
import blue.origami.nez.ast.Source;
import blue.origami.nez.ast.SourcePosition;
import blue.origami.nez.ast.Symbol;
import blue.origami.nez.ast.Tree;
import blue.origami.parser.Parser;
import blue.origami.parser.ParserSource;
import blue.origami.parser.peg.Grammar;
import blue.origami.parser.peg.GrammarParser;
import blue.origami.util.OConsole;
import blue.origami.util.ODebug;
import blue.origami.util.OOption;

public class Oexample extends Main {
	HashMap<String, Parser> parserMap = new HashMap<>();
	HashMap<String, Long> timeMap = new HashMap<>();
	OTreeWriter treeWriter = null;

	Coverage cov = null;
	String desc = "";
	int tested = 0;
	int succ = 0;

	@Override
	protected void initOption(OOption options) {
		super.initOption(options);
		options.set(MainOption.ThrowingParserError, true);
		// options.set(ParserOption.PartialFailure, true);
	}

	@Override
	public void exec(OOption options) throws Throwable {
		Grammar g = this.getGrammar(options);
		// g.dump();
		this.treeWriter = options.newInstance(OTreeWriter.class);
		if (options.is(MainOption.Coverage, false)) {
			this.cov = new Coverage();
			this.cov.init(options, g);
		}
		this.loadExample(options, g);
		if (this.tested > 0) {
			double passRatio = (double) this.succ / this.tested;
			if (this.cov != null) {
				beginColor(Yellow);
				this.cov.dump(options);
				endColor();
				double fullcov = this.cov.cov();
				p(bold("Result: %.2f%% passed, %.2f%% (coverage) tested."), (passRatio * 100), (fullcov * 100));
				if (this.tested == this.succ && fullcov > 0.5) {
					p("");
					p(bold("Congratulation!!"));
					p("You are invited to share your grammar at Nez open grammar repository, ");
					p(" http://github.com/nez-peg/grammar.");
					p("If you want, please send a pull-request with:");
					p(bold("git commit -m '" + this.desc + ", %.2f%% (coverage) tested.'"), (fullcov * 100));
				}
			} else {
				p(bold("Result: %.2f%% passed."), (passRatio * 100));
			}
		}
	}

	void loadExample(OOption options, Grammar g) throws IOException {
		String path = options.stringValue(MainOption.GrammarFile, null);
		if (path == null) {
			exit(1, MainFmt.no_specified_grammar);
		}
		Source s = ParserSource.newFileSource(path, options.stringList(MainOption.GrammarPath));
		this.importFile(g, s, options);
		this.desc = parseGrammarDescription(s);
	}

	void importFile(Grammar g, Source s, OOption options) throws IOException {
		Tree<?> t = GrammarParser.OPegParser.parse(s);
		if (t.is(GrammarParser._Source)) {
			for (Tree<?> sub : t) {
				this.parse(g, sub, options);
			}
		}
	}

	public final static Symbol _Example = Symbol.unique("Example");
	public final static Symbol _hash = Symbol.unique("hash"); // example
	public final static Symbol _name2 = Symbol.unique("name2"); // example
	public final static Symbol _text = Symbol.unique("text"); // example

	void parse(Grammar g, Tree<?> node, OOption options) throws IOException {
		if (node.is(GrammarParser._Production)) {
			return;
		}
		if (node.is(_Example)) {
			this.parseExample(node, g, options);
			return;
		}
		if (node.is(GrammarParser._Grammar)) {
			String name = node.getStringAt(GrammarParser._name, null);
			Grammar lg = g.getGrammar(name);
			Tree<?> body = node.get(GrammarParser._body);
			for (Tree<?> sub : body) {
				this.parse(lg, sub, options);
			}
			return;
		}
		if (node.is(GrammarParser._Import)) {
			String name = node.getStringAt(GrammarParser._name, null);
			String path = name;
			if (!name.startsWith("/") && !name.startsWith("\\")) {
				path = SourcePosition.extractFilePath(node.getSource().getResourceName()) + "/" + name;
			}
			this.importFile(g, ParserSource.newFileSource(path, null), options);
			return;
		}
	}

	public void parseExample(Tree<?> node, Grammar g, OOption options) throws IOException {
		Tree<?> nameNode = node.get(GrammarParser._name, null);
		String uname = nameNode.getString();
		Parser p = this.getParser(g, uname, nameNode, options);
		if (p != null) {
			this.performExample(p, uname, node);
		}
		if (this instanceof Otest) {
			nameNode = node.get(_name2, null);
			if (nameNode != null) {
				uname = nameNode.getString();
				p = this.getParser(g, uname, nameNode, options);
				if (p != null) {
					this.performExample(p, uname, node);
				}
			}
		}
	}

	private Parser getParser(Grammar g, String name, Tree<?> nameNode, OOption options) throws IOException {
		String uname = g.getUniqueName(name);
		Parser p = this.parserMap.get(uname);
		if (p == null) {
			options.set(MainOption.Start, name);
			p = g.newParser(options);
			if (p == null) {
				options.reportError(nameNode, "undefined nonterminal: %s", name);
				return null;
			}
			this.parserMap.put(uname, p);
		}
		return p;
	}

	protected void performExample(Parser p, String uname, Tree<?> ex) {
		Tree<?> textNode = ex.get(_text);
		Source s = this.newSource(textNode);
		String name = uname + " (" + textNode.getSource().getResourceName() + ":"
				+ textNode.getSource().linenum(textNode.getSourcePosition()) + ")";
		try {
			this.tested++;
			long t1 = System.nanoTime();
			Tree<?> node = p.parse(s);
			this.succ++;
			long t2 = System.nanoTime();
			p(Green, "[PASS] " + name);
			if (!(this instanceof Otest)) {
				if (node != null) {
					OConsole.dump(" ", bold(textNode.getString()));
					display(this.treeWriter, node);
				}
			}
			this.record(uname, t2 - t1);
		} catch (IOException e) {
			p(Red, e.toString());
			p(Red, "[FAIL] " + name);
			p(Red, bold(textNode.getString()));
		} catch (Throwable e) {
			p(Red, "[FAIL] " + name);
			p(Red, bold(textNode.getString()));
			e.printStackTrace();
			ODebug.traceException(e);
		}
	}

	private void record(String uname, long t) {
		Long l = this.timeMap.get(uname);
		if (l == null) {
			l = t;
		} else {
			l += t;
		}
		this.timeMap.put(uname, l);
	}

	private Source newSource(Tree<?> textNode) {
		// byte[] b = parseBinary(textNode.toText());
		// if (b != null) {
		// return new StringSource(textNode.getSource().getResourceName(),
		// textNode.getSourcePosition(), b, true);
		// }
		return textNode.toSource();
	}

	// private byte[] parseBinary(String t) {
	// ArrayList<Byte> bytes = new ArrayList<>(t.length());
	// for (int i = 0; i < t.length(); i++) {
	// char ch = t.charAt(i);
	// if (ch == ' ' || ch == '\n' || ch == '\r') {
	// continue;
	// }
	// int b = parseHex(t, i);
	// if (b != -1) {
	// // System.out.println("hex=" + b);
	// i += 1;
	// bytes.add((byte) b);
	// continue;
	// }
	// }
	// bytes.add((byte) 0);
	// byte[] b = new byte[bytes.size()];
	// for (int i = 0; i < b.length; i++) {
	// b[i] = bytes.get(i);
	// }
	// return b;
	// }
	//
	// private int parseHex(String t, int i) {
	// try {
	// char ch = t.charAt(i);
	// char ch2 = t.charAt(i + 1);
	// return Integer.parseInt("" + ch + ch2, 16);
	//
	// } catch (Exception e) {
	// return -1;
	// }
	// }

	public final static String parseGrammarDescription(Source sc) {
		StringBuilder sb = new StringBuilder();
		long pos = 0;
		boolean found = false;
		for (; pos < sc.length(); pos++) {
			int ch = sc.byteAt(pos);
			if (Character.isAlphabetic(ch)) {
				found = true;
				break;
			}
		}
		if (found) {
			for (; pos < sc.length(); pos++) {
				int ch = sc.byteAt(pos);
				if (ch == '\n' || ch == '\r' || ch == '-' || ch == '*') {
					break;
				}
				sb.append((char) ch);
			}
		}
		return sb.toString().trim();
	}

}
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import blue.origami.common.OConsole;
import blue.origami.common.OOption;
import blue.origami.main.MainFmt;
import blue.origami.main.MainOption;
import origami.nez2.Expr;
import origami.nez2.PEG;
import origami.nez2.PEG.Unary;
import origami.nez2.ParseTree;
import origami.nez2.Parser;

public class Oparse extends Main {

	@Override
	protected void initOption(OOption options) {
		super.initOption(options);
		options.set(MainOption.ThrowingParserError, false);
		options.set(MainOption.PartialFailure, true);
	}

	@Override
	public void exec(OOption options) throws Throwable {
		PEG peg = new PEG();
		peg.load(pegFile(options, null));
		String[] files = options.stringList(MainOption.InputFiles);
		if (files.length > 0) {
			Parser p = peg.getParser();
			for (String file : files) {
				long st = System.nanoTime();
				Object t = p.parseFile(file);
				long et = System.nanoTime();
				System.err.printf("%s %f[ms]: ", file, (et - st) / 1000000.0);
				if (files.length == 1) {
					System.out.print(t);
					System.out.flush();
				}
				System.err.printf("\n");
			}
		} else {
			this.exec2(peg, options);
		}
	}

	public void exec2(PEG peg, OOption options) throws Throwable {
		this.displayVersion();
		this.c(Yellow, () -> {
			p("Enter an input string to match (or a grammar if you want to update).");
			p("Tips: Start with an empty line for multiple lines.");
			p(" Entering two empty lines diplays the current grammar.");
		});
		OConsole.println("");
		// Parser nezParser = PEG.nez().getParser();
		Parser p = peg.getParser(pegStart(peg, options));
		String prompt = this.getPrompt(p);
		String input = null;
		while ((input = this.readMulti(prompt)) != null) {
			if (checkEmptyInput(input)) {
				System.out.println(peg);
				continue;
			}
			// try {
			// ParseTree node = nezParser.parse(input);
			// if (node != null && node.is("Source")) {
			// g = SourceGrammar.loadSource(sc);
			// p = this.newParser(g, options);
			// prompt = this.getPrompt(g);
			// this.addHistory(input);
			// p(Yellow, MainFmt.grammar_is_successfully_loaded);
			// continue;
			// }
			// } catch (Exception e) {
			// // ODebug.traceException(e);
			// }
			ParseTree node = p.parse(input);
			OConsole.println(node);
		}
	}

	static String find(String file, String... paths) {
		// if (file == null) {
		// return null;
		// }
		File f = new File(file);
		if (f.isFile()) {
			return file;
		}
		InputStream stream = Main.class.getResourceAsStream(file);
		if (stream != null) {
			try {
				stream.close();
			} catch (Exception e) {
			}
			return file;
		}
		if (paths.length > 0) {
			for (String f0 : paths) {
				String f2 = f0 + (f0.endsWith("/") ? file : "/" + file);
				f2 = find(f2);
				if (f2 != null) {
					return f2;
				}
			}
		}
		return null;
	}

	public static String pegFile(OOption options, String file) {
		String pegfile = options.stringValue(MainOption.GrammarFile, file);
		if (pegfile != null) {
			pegfile = find(pegfile, options.stringList(MainOption.GrammarPath));
		}
		if (pegfile == null) {
			exit(1, MainFmt.no_specified_grammar);
		}
		return pegfile;
	}

	static String pegStart(PEG peg, OOption options) {
		String start = options.stringValue(MainOption.Start, null);
		return start == null ? peg.getStart() : start;
	}

	private String getPrompt(Parser p) throws IOException {
		String start = p.toString();
		return this.c(Bold, start + ">>> ");
	}

	static class Coverage {
		final String[] names;
		final int[] enterCounts;
		final int[] exitCounts;

		public Coverage(PEG peg) {
			ArrayList<String> nameList = new ArrayList<>();
			peg.forEach(n -> {
				nameList.add(n);
			});
			this.names = nameList.toArray(new String[nameList.size()]);
			this.enterCounts = new int[nameList.size()];
			this.exitCounts = new int[nameList.size()];
			for (int i = 0; i < nameList.size(); i++) {
				String n = nameList.get(i);
				Expr pe = peg.get(n);
				peg.set(n, new Unary("cov", pe, Unary.cov(this.enterCounts, this.exitCounts, i)));
			}
		}

		public final double enterCov() {
			int c = 0;
			for (int i = 0; i < this.names.length; i++) {
				if (this.enterCounts[i] > 0) {
					c++;
				}
			}
			return ((double) c) / this.names.length;
		}

		public final double cov() {
			int c = 0;
			for (int i = 0; i < this.names.length; i++) {
				if (this.exitCounts[i] > 0) {
					c++;
				}
			}
			return ((double) c) / this.names.length;
		}

		public void dump() {
			for (int i = 0; i < this.names.length; i++) {
				if (this.exitCounts[i] == 0) {
					OConsole.println("%s %d/%d", this.names[i], this.enterCounts[i], this.exitCounts[i]);
				}
			}
			OConsole.println("Coverage %.3f %.3f", this.enterCov(), this.cov());
		}
	}
}

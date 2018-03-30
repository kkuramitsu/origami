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

import java.io.File;
import java.io.InputStream;

import blue.origami.common.OOption;
import origami.nez2.PEG;
import origami.nez2.Parser;

public class Oparse2 extends Main {

	@Override
	protected void initOption(OOption options) {
		super.initOption(options);
		options.set(MainOption.ThrowingParserError, false);
		options.set(MainOption.PartialFailure, true);
	}

	static String find(String file, String... paths) {
		if (file == null) {
			return null;
		}
		File f = new File(file);
		if (f.isFile()) {
			return file;
		}
		InputStream stream = f.getClass().getResourceAsStream(file);
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

	@Override
	public void exec(OOption options) throws Throwable {
		String pegfile = find(options.stringValue(MainOption.GrammarFile, null),
				options.stringList(MainOption.GrammarPath));
		if (pegfile == null) {
			exit(1, MainFmt.no_specified_grammar);
		}
		PEG peg = new PEG();
		peg.load(pegfile);
		String[] files = options.stringList(MainOption.InputFiles);
		if (files.length > 0) {
			Parser p = peg.getParser();
			for (String file : files) {
				long st = System.nanoTime();
				Object t = p.parseFile(file);
				long et = System.nanoTime();
				System.err.printf("%s %f[ms]: ", file, (et - st) / 1000000.0);
				// System.out.print(t);
				System.out.flush();
				System.err.printf("\n");
			}
		}
	}

	// public void exec2(OOption options) throws Throwable {
	// this.displayVersion();
	// p(Yellow, "Enter an input string to match (or a grammar if you want to
	// update).");
	// p(Yellow, "Tips: Start with an empty line for multiple lines.");
	// p(Yellow, " Entering two empty lines diplays the current grammar.");
	// OConsole.println("");
	// Parser nezParser = GrammarParser.OPegParser;
	//
	// Grammar g = this.getGrammar(options);
	// Parser p = this.newParser(g, options);
	// OTreeWriter tw = options.newInstance(OTreeWriter.class);
	// String prompt = this.getPrompt(g);
	// String input = null;
	// while ((input = this.readMulti(prompt)) != null) {
	// if (checkEmptyInput(input)) {
	// g.dump();
	// continue;
	// }
	// OSource sc = ParserSource.newStringSource("<stdio>", this.linenum, input);
	// try {
	// Tree<?> node = nezParser.parse(sc);
	// if (node != null && node.is(GrammarParser._Source)) {
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
	// Tree<?> node = p.parse(sc);
	// if (node == null) {
	// p(Red, MainFmt.Tips__starting_with_an_empty_line_for_multiple_lines);
	// } else {
	// display(tw, node);
	// }
	// }
	// }
	//
	// @Override
	// protected Grammar getGrammar(OOption options, String file) throws IOException
	// {
	// file = options.stringValue(MainOption.GrammarFile, file);
	// if (file == null) {
	// return new SourceGrammar();
	// }
	// return SourceGrammar.loadFile(file,
	// options.stringList(MainOption.GrammarPath));
	// }
	//
	// private Parser newParser(Grammar g, OOption options) throws IOException {
	// Parser p = g.newParser(options);
	// return p;
	// }
	//
	// private String getPrompt(Grammar g) throws IOException {
	// String start = g.getStartProduction().getLocalName();
	// return bold(start + ">>> ");
	// }

}

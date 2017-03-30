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

import blue.origami.nez.ast.Source;
import blue.origami.nez.ast.Tree;
import blue.origami.nez.parser.Parser;
import blue.origami.nez.parser.ParserOption;
import blue.origami.nez.parser.ParserSource;
import blue.origami.nez.peg.Grammar;
import blue.origami.nez.peg.GrammarParser;
import blue.origami.util.OConsole;
import blue.origami.util.OOption;

public class Onez extends OCommand {

	protected void initOption(OOption options) {
		super.initOption(options);
		options.set(ParserOption.ThrowingParserError, false);
	}

	@Override
	public void exec(OOption options) throws Throwable {
		OCommand.displayVersion("Nez");
		p(Yellow, "Enter an input string to match (or a grammar if you want to update).");
		p(Yellow, "Tips: Start with an empty line for multiple lines.");
		p(Yellow, " Entering two empty lines diplays the current grammar.");
		OConsole.println("");
		Parser nezParser = GrammarParser.NezParser;
		// nezParser.setPrintingException(false);
		// nezParser.setThrowingException(false);

		Grammar g = getGrammar(options);
		Parser p = newParser(g, options);
		OTreeWriter tw = options.newInstance(OTreeWriter.class);
		String prompt = getPrompt(g);
		String input = null;
		while ((input = this.readMulti(prompt)) != null) {
			if (checkEmptyInput(input)) {
				g.dump();
				continue;
			}
			Source sc = ParserSource.newStringSource("<stdio>", linenum, input);
			try {
				Tree<?> node = nezParser.parse(sc);
				if (node != null && node.is(GrammarParser._Source)) {
					g = Grammar.loadSource(sc);
					p = newParser(g, options);
					prompt = getPrompt(g);
					addHistory(input);
					p(Yellow, "Grammar is successfully loaded!");
					continue;
				}
			} catch (Exception e) {
				// ODebug.traceException(e);
			}
			Tree<?> node = p.parse(sc);
			if (node == null) {
				p(Red, MainFmt.Tips__starting_with_an_empty_line_for_multiple_lines);
			} else {
				display(tw, node);
			}
		}
	}

	private Parser newParser(Grammar g, OOption options) throws IOException {
		Parser p = g.newParser(options);
		return p;
	}

	private String getPrompt(Grammar g) throws IOException {
		String start = g.getStartProduction().getLocalName();
		return bold(start + ">>> ");
	}

}

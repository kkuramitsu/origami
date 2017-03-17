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

import java.io.IOException;

import origami.OConsole;
import origami.ODebug;
import origami.nez.ast.Source;
import origami.nez.ast.Tree;
import origami.nez.parser.CommonSource;
import origami.nez.parser.Parser;
import origami.nez.parser.ParserFactory;
import origami.nez.parser.ParserFactory.GrammarWriter;
import origami.nez.parser.ParserFactory.TreeWriter;
import origami.nez.peg.GrammarLoader;
import origami.nez.peg.GrammarParser;
import origami.nez.peg.OGrammar;
import origami.trait.OVerbose;

public class Oinez extends OCommand {
	@Override
	public void exec(ParserFactory fac) throws IOException {
		OCommand.displayVersion();
		p(Yellow, "Enter an input string to match (or a grammar if you want to update).");
		p(Yellow, "Tips: Start with an empty line for multiple lines.");
		p(Yellow, " Entering two empty lines diplays the current grammar.");
		OConsole.println("");
		Parser nezParser = GrammarParser.NezParser;
		nezParser.setPrintingException(false);
		nezParser.setThrowingException(false);
		Parser pegParser = parser(fac);
		GrammarWriter gw = fac.newGrammarWriter(origami.main.tool.SimpleGrammarWriter.class);
		TreeWriter tw = fac.newTreeWriter(origami.main.tool.AbstractSyntaxTreeWriter.class);
		String prompt = prompt(fac);
		String input = null;
		while ((input = this.readMulti(prompt)) != null) {
			if (checkEmptyInput(input)) {
				display(fac, gw, fac.getGrammar());
				continue;
			}
			Source sc = CommonSource.newStringSource("<stdio>", linenum, input);
			try {
				Tree<?> node = nezParser.parse(sc);
				if (node != null && node.is(GrammarParser._Source)) {
					OGrammar g = OGrammar.loadSource(sc);
					fac = fac.newFactory(g);
					pegParser = parser(fac);
					prompt = prompt(fac);
					addHistory(input);
					p(Yellow, "Grammar is successfully loaded!");
					continue;
				}
			} catch (Exception e) {
				ODebug.traceException(e);
			}
			Tree<?> node = pegParser.parse(sc);
			if (node == null) {
				p(Red, "Tips: To enter multiple lines, start and end an empty line.");
			} else {
				display(fac, tw, node);
			}
		}
	}

	private Parser parser(ParserFactory fac) throws IOException {
		Parser p = fac.newParser();
		p.setPrintingException(true);
		p.setThrowingException(false);
		return p;
	}

	private String prompt(ParserFactory fac) throws IOException {
		OGrammar g = fac.getGrammar();
		String start = g.getStartProduction().getLocalName();
		return bold(start + ">>> ");
	}

}

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

import blue.origami.OrigamiContext;
import blue.origami.nez.ast.SourcePosition;
import blue.origami.nez.parser.ParserOption;
import blue.origami.nez.peg.Grammar;
import blue.origami.util.ODebug;
import blue.origami.util.OOption;

public class Orun extends blue.origami.main.OCommand {

	@Override
	public void exec(OOption options) throws Throwable {
		String[] files = options.list(ParserOption.InputFiles);
		if (options.value(ParserOption.GrammarFile, null) == null) {
			if (files.length > 0) {
				String ext = SourcePosition.extractFileExtension(files[0]);
				options.set(ParserOption.GrammarFile, ext + ".opeg");
			}
		}
		Grammar g = this.getGrammar(options, "iroha.opeg");
		OrigamiContext env = new OrigamiContext(g, options);
		ODebug.setDebug(this.isDebug());

		for (String file : files) {
			env.loadScriptFile(file);
		}
		if (files.length == 0 || this.isDebug()) {
			displayVersion(g.getName());
			p(Yellow, MainFmt.Tips__starting_with_an_empty_line_for_multiple_lines);
			p("");

			int startline = this.linenum;
			String prompt = bold(">>> ");
			String input = null;
			while ((input = this.readMulti(prompt)) != null) {
				if (checkEmptyInput(input)) {
					continue;
				}
				env.shell("<stdin>", startline, input);
				startline = this.linenum;
			}
		}
	}

	public boolean isDebug() {
		return false;
	}
}

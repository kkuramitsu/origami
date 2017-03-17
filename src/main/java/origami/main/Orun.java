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

import origami.ODebug;
import origami.Origami;
import origami.nez.parser.CommonSource;
import origami.nez.parser.ParserFactory;

public class Orun extends origami.main.OCommand {

	@Override
	public void exec(ParserFactory fac) throws IOException {
		String[] files = fac.list("files");
		if (fac.value("grammar", null) == null) {
			if (files.length > 0) {
				String ext = CommonSource.extractFileExtension(files[0]);
				fac.set("grammar", ext + ".nez");
			} else {
				fac.set("grammar", "iroha.nez");
			}
		}
		Origami env = new Origami(fac.getGrammar());
		ODebug.setDebug(this.isDebug());
		// env.importClass(origami.rule.IrohaSet.class);
		// importClass(env, fac.get("grammar"));

		if (files.length > 0) {
			for (String file : files) {
				env.loadScriptFile(file);
			}
		}
		if (files.length == 0 || isDebug()) {
			displayVersion();
			p(Yellow, "Enter an input string to parse and run.");
			p(Yellow, "Tips: Start with an empty line for multiple lines.");
			p("");

			int startline = linenum;
			String prompt = bold(">>> ");
			String input = null;
			while ((input = this.readMulti(prompt)) != null) {
				if (checkEmptyInput(input)) {
					continue;
				}
				env.shell("<stdin>", startline, input);
				startline = linenum;
			}
		}
	}

	public boolean isDebug() {
		return false;
	}
}

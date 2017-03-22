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

import java.util.ArrayList;

import origami.nez.ast.Source;
import origami.nez.parser.ParserSource;
import origami.nez.parser.Parser;

public class Omatch extends OCommand {
	protected void initOption(OOption options) {
		super.initOption(options);
		options.set(ParserOption.ThrowingParserError, false);		
		options.set(ParserOption.TreeConstruction, false);
	}

	@Override
	public void exec(OOption options) throws Throwable {
		Parser parser = getParser(options);
		if (options.value(ParserOption.InlineGrammar, null) != null) {
			String t = options.value(ParserOption.InlineGrammar, null);
			Source input = ParserSource.newStringSource(t);
			int l = parser.match(input);
			if (l == -1) {
				p("failed: %s", t);
				System.exit(1);
			}
			return;
		}
		String[] files = options.list(ParserOption.InputFiles);
		ArrayList<String> failedFileList = new ArrayList<>();
		this.checkInputSource(files);
		for (String file : files) {
			Source input = ParserSource.newFileSource(file, null);
			int l = parser.match(input);
			if (l == -1) {
				failedFileList.add(file);
			}
		}
		if (failedFileList.size() > 0) {
			p("failed: %s", failedFileList);
			System.exit(1);
		}
	}
}
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

import blue.nez.ast.Source;
import blue.nez.ast.Tree;
import blue.nez.parser.Parser;
import blue.nez.parser.ParserOption;
import blue.nez.parser.ParserSource;
import blue.origami.main.tool.OTreeWriter;
import blue.origami.util.OOption;

public class Oparse2 extends OCommand {

	protected void initOption(OOption options) {
		super.initOption(options);
		options.set(ParserOption.ThrowingParserError, false);
	}

	@Override
	public void exec(OOption options) throws Throwable {
		Parser parser = getParser(options);

		OTreeWriter treeWriter = options.newInstance(OTreeWriter.class);
		treeWriter.init(options);
		if (options.stringValue(ParserOption.InlineGrammar, null) != null) {
			Source input = ParserSource.newStringSource(options.stringValue(ParserOption.InlineGrammar, null));
			Tree<?> node = parser.parse(input);
			if (node != null) {
				treeWriter.write(node);
			}
		}
		String[] files = options.stringList(ParserOption.InputFiles);
		this.checkInputSource(files);
		for (String file : files) {
			Source input = ParserSource.newFileSource(file, null);
			Tree<?> node = parser.parse(input);
			if (node != null) {
				treeWriter.write(node);
			}
		}
		treeWriter.close();
	}

}

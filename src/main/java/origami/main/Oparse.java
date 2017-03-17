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

import origami.main.tool.LineTreeWriter;
import origami.nez.ast.Source;
import origami.nez.ast.Tree;
import origami.nez.parser.CommonSource;
import origami.nez.parser.Parser;
import origami.nez.parser.ParserFactory;
import origami.nez.parser.ParserFactory.TreeWriter;
//import origami.nez.tool.LineTreeWriter;

public class Oparse extends OCommand {
	@Override
	public void exec(ParserFactory fac) throws IOException {
		Parser parser = fac.newParser();
		parser.setThrowingException(false);
		parser.setPrintingException(true);
		TreeWriter treeWriter = fac.newTreeWriter(origami.main.tool.AbstractSyntaxTreeWriter.class);
		if (fac.value("text", null) != null) {
			Source input = CommonSource.newStringSource(fac.value("text", null));
			Tree<?> node = parser.parse(input);
			if (node != null) {
				treeWriter.writeTree(fac, node);
			}
		}
		String[] files = fac.list("files");
		this.checkInputSource(files);
		for (String file : files) {
			Source input = CommonSource.newFileSource(file, null);
			Tree<?> node = parser.parse(input);
			if (node != null) {
				treeWriter.writeTree(fac, node);
			}
			if (node == null && treeWriter instanceof LineTreeWriter) {
				p("null");
			}
		}
		treeWriter.close();
	}
}

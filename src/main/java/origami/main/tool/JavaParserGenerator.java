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

package origami.main.tool;

import origami.nez.peg.Grammar;

public class JavaParserGenerator extends ParserGenerator {

	@Override
	protected void initLanguageSpec() {
		// this.UniqueNumberingSymbol = false;
		this.addType("$parse", "boolean");
		this.addType("$tag", "int");
		this.addType("$label", "int");
		this.addType("$table", "int");
		this.addType("$text", "byte[]");
		this.addType("$index", "byte[]");
		this.addType("$set", "boolean[]");
		this.addType("$string", "String[]");

		this.addType("memo", "int");
		this.addType(_set(), "boolean[]");
		this.addType(_index(), "byte[]");
		this.addType(_temp(), "boolean");
		this.addType(_pos(), "int");
		this.addType(_tree(), "T");
		this.addType(_log(), "int");
		this.addType(_table(), "int");
		this.addType(_state(), "ParserContext<T>");
	}

	@Override
	protected void generateHeader(Grammar g) {
		Statement("import java.io.IOException");
		Statement("import java.nio.charset.StandardCharsets");
		Statement("import java.nio.file.*");
		BeginDecl("public class " + _basename());
		importFileContent("java-parser-runtime.txt");
	}

	@Override
	protected void generateFooter(Grammar g) {
		BeginDecl("public final static void main(String[] a)");
		{
			Statement("jnez_main(a)");
			// Statement("SimpleTree t = parse(a[0])");
			// Statement("System.out.println(t)");
		}
		EndDecl();
		BeginDecl("public final static boolean start(ParserContext<?> c)");
		{
			Return(_funccall(_funcname(g.getStartProduction())));
		}
		EndDecl();
		EndDecl(); // end of class
		L("/*EOF*/");
		this.showManual("jnez-man.txt", new String[] { "$cmd$", _basename() });
	}

	@Override
	protected String _defun(String type, String name) {
		return "private static <T> " + type + " " + name;
	}

}

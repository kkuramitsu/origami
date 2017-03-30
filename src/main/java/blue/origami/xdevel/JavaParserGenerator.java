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

package blue.origami.xdevel;

import blue.nez.peg.Grammar;

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
		this.addType(this._set(), "boolean[]");
		this.addType(this._index(), "byte[]");
		this.addType(this._temp(), "boolean");
		this.addType(this._pos(), "int");
		this.addType(this._tree(), "T");
		this.addType(this._log(), "int");
		this.addType(this._table(), "int");
		this.addType(this._state(), "ParserContext<T>");
	}

	@Override
	protected void generateHeader(Grammar g) {
		this.Statement("import java.io.IOException");
		this.Statement("import java.nio.charset.StandardCharsets");
		this.Statement("import java.nio.file.*");
		this.BeginDecl("public class " + this._basename());
		this.importFileContent("java-parser-runtime.txt");
	}

	@Override
	protected void generateFooter(Grammar g) {
		this.BeginDecl("public final static void main(String[] a)");
		{
			this.Statement("jnez_main(a)");
			// Statement("SimpleTree t = parse(a[0])");
			// Statement("System.out.println(t)");
		}
		this.EndDecl();
		this.BeginDecl("public final static boolean start(ParserContext<?> c)");
		{
			this.Return(this._funccall(this._funcname(g.getStartProduction())));
		}
		this.EndDecl();
		this.EndDecl(); // end of class
		this.L("/*EOF*/");
		this.showResourceContent("jnez-man.txt", new String[] { "$cmd$", this._basename() });
	}

	@Override
	protected String _defun(String type, String name) {
		return "private static <T> " + type + " " + name;
	}

}

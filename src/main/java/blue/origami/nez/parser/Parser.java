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

package blue.origami.nez.parser;

import java.io.IOException;

import blue.origami.nez.ast.CommonTree;
import blue.origami.nez.ast.Source;
import blue.origami.nez.ast.Symbol;
import blue.origami.nez.ast.Tree;
import blue.origami.nez.parser.pasm.PAsmAPI.TreeFunc;
import blue.origami.nez.parser.pasm.PAsmAPI.TreeSetFunc;
import blue.origami.nez.parser.pasm.PAsmCompiler;
import blue.origami.nez.peg.Production;
import blue.origami.util.OOption;

public final class Parser {

	private final Production start;
	private final OOption options;
	private ParserCode compiledParser = null;

	public Parser(Production start, OOption options) {
		this.options = options;
		this.start = start;
		assert start != null;
	}

	public final ParserGrammar getParserGrammar() {
		if (this.compiledParser == null) {
			this.compile();
		}
		return this.compiledParser.getParserGrammar();
	}

	public final ParserCode compile() {
		ParserCompiler compl = this.options.newInstance(PAsmCompiler.class);
		long t = this.options.nanoTime(null, 0);
		ParserGrammar g = new ParserChecker(this.options, this.start).checkParserGrammar();
		this.compiledParser = compl.compile(g);
		this.options.nanoTime("ParserCompilingTime@" + this.start.getUniqueName(), t);
		return this.compiledParser;
	}

	public final ParserCode getExecutable() {
		if (this.compiledParser == null) {
			this.compile();
		}
		return this.compiledParser;
	}

	/* --------------------------------------------------------------------- */

	public final Object parse(Source s, int pos, TreeFunc newTree, TreeSetFunc linkTree) throws IOException {
		ParserCode parser = this.getExecutable();
		return parser.parse(s, pos, newTree, linkTree);
	}

	public final long match(Source s, int pos) {
		ParserCode parser = this.getExecutable();

		return parser.match(s, pos, //
				(Symbol tag, Source s0, int spos, int epos, int nsubs, Object value) -> null, //
				(Object parent, int index, Symbol label, Object child) -> null);
	}

	/* wrapper */

	public final int match(Source s) {
		return (int) this.match(s, 0);
	}

	public final int match(String str) {
		return this.match(ParserSource.newStringSource(str));
	}

	private static CommonTree defaultTree = new CommonTree();

	public final Tree<?> parse(Source sc) throws IOException {
		return (CommonTree) this.parse(sc, 0, defaultTree, defaultTree);
	}

	public final Tree<?> parse(String str) throws IOException {
		return this.parse(ParserSource.newStringSource(str));
	}
}

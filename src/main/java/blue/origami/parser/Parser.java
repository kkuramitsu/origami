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

package blue.origami.parser;

import java.io.IOException;

import blue.origami.common.CommonTree;
import blue.origami.common.OOption;
import blue.origami.common.OSource;
import blue.origami.common.Symbol;
import blue.origami.common.Tree;
import blue.origami.parser.pasm.PAsmCompiler;
import blue.origami.parser.pasm.PAsmAPI.TreeFunc;
import blue.origami.parser.pasm.PAsmAPI.TreeSetFunc;
import blue.origami.parser.peg.Production;

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

	public final Object parse(OSource s, int pos, TreeFunc newTree, TreeSetFunc linkTree) throws IOException {
		ParserCode parserCode = this.getExecutable();
		return parserCode.parse(s, pos, newTree, linkTree);
	}

	public final long match(OSource s, int pos) {
		ParserCode parser = this.getExecutable();

		return parser.match(s, pos, //
				(Symbol tag, OSource s0, int spos, int epos, int nsubs, Object value) -> null, //
				(Object parent, int index, Symbol label, Object child) -> null);
	}

	/* wrapper */

	public final int match(OSource s) {
		return (int) this.match(s, 0);
	}

	public final int match(String str) {
		return this.match(ParserSource.newStringSource(str));
	}

	private static CommonTree defaultTree = new CommonTree();

	public final Tree<?> parse(OSource sc) throws IOException {
		return (CommonTree) this.parse(sc, 0, defaultTree, defaultTree);
	}

	public final Tree<?> parse(String str) throws IOException {
		return this.parse(ParserSource.newStringSource(str));
	}
}

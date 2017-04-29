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

package blue.nez.parser;

import java.io.IOException;

import blue.nez.ast.CommonTree;
import blue.nez.ast.LocaleFormat;
import blue.nez.ast.Source;
import blue.nez.ast.SourcePosition;
import blue.nez.ast.Tree;
import blue.nez.peg.NezFmt;
import blue.nez.peg.Production;
import blue.origami.util.OOption;

public final class Parser {

	private final Production start;
	private final OOption options;
	private ParserExecutable compiledParserCode = null;

	public Parser(Production start, OOption options) {
		this.options = options;
		this.start = start;
		assert start != null;
	}

	public final ParserGrammar getParserGrammar() {
		if (this.compiledParserCode == null) {
			this.compile();
		}
		return this.compiledParserCode.getGrammar();
	}

	public final ParserExecutable compile() {
		ParserCompiler compl = this.options.newInstance(PegAsmCompiler.class);
		long t = this.options.nanoTime(null, 0);
		ParserGrammar g = new ParserChecker(this.options, this.start).checkParserGrammar();
		this.compiledParserCode = compl.compile(g);
		this.options.nanoTime("ParserCompilingTime@" + this.start.getUniqueName(), t);
		return this.compiledParserCode;
	}

	public final ParserExecutable getExecutable() {
		if (this.compiledParserCode == null) {
			this.compile();
		}
		return this.compiledParserCode;
	}

	/* --------------------------------------------------------------------- */

	final <T> T exec(ParserContext<T> px, Source s) {
		px.start();
		T matched = this.compiledParserCode.exec(px);
		px.end();
		return matched;
	}

	public final <T> T parse(Source s, long pos, TreeConstructor<T> newTree, TreeConnector<T> linkTree)
			throws IOException {
		ParserExecutable parser = this.getExecutable();
		ParserContext<T> ctx = parser.newContext(s, pos, newTree, linkTree);
		T matched = this.exec(ctx, s);
		if (matched == null) {
			this.perror(SourcePosition.newInstance(s, ctx.getMaximumPosition()), NezFmt.syntax_error);
			return null;
		}
		if (!ctx.eof() && this.options.is(ParserOption.PartialFailure, false)) {
			this.pwarn(SourcePosition.newInstance(s, ctx.getPosition()), NezFmt.unconsumed);
		}
		return matched;
	}

	public final <T> long match(Source s, long pos, TreeConstructor<T> newTree, TreeConnector<T> linkTree) {
		ParserExecutable parser = this.getExecutable();
		ParserContext<T> ctx = parser.newContext(s, pos, newTree, linkTree);
		T matched = this.exec(ctx, s);
		if (matched == null) {
			return -1;
		}
		return ctx.getPosition();
	}

	/* wrapper */

	private static CommonTree defaultTree = new CommonTree();

	public final int match(Source s) {
		return (int) this.match(s, 0, defaultTree, defaultTree);
	}

	public final int match(String str) {
		return this.match(ParserSource.newStringSource(str));
	}

	public final Tree<?> parse(Source sc) throws IOException {
		return this.parse(sc, 0, defaultTree, defaultTree);
	}

	public final Tree<?> parse(String str) throws IOException {
		return this.parse(ParserSource.newStringSource(str));
	}

	/* Error Handling */

	private void perror(SourcePosition s, LocaleFormat message) throws IOException {
		if (this.options.is(ParserOption.ThrowingParserError, true)) {
			throw new ParserErrorException(s, message);
		} else {
			this.options.reportError(s, message);
		}
	}

	private void pwarn(SourcePosition s, LocaleFormat message) throws IOException {
		if (this.options.is(ParserOption.ThrowingParserError, true)) {
			throw new ParserErrorException(s, message);
		} else {
			this.options.reportWarning(s, message);
		}
	}

	@SuppressWarnings("serial")
	public static class ParserErrorException extends IOException {
		final SourcePosition s;
		final LocaleFormat message;

		public ParserErrorException(SourcePosition s, LocaleFormat message) {
			this.s = s;
			this.message = message;
		}

		@Override
		public final String toString() {
			return SourcePosition.formatErrorMessage(this.s, this.message);
		}
	}

}

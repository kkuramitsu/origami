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

package origami.nez.parser;

import java.io.IOException;

import origami.nez.ast.CommonTree;
import origami.nez.ast.Source;
import origami.nez.ast.Tree;
import origami.nez.peg.OGrammar;
import origami.nez.peg.OProduction;

public final class Parser {
	private final OProduction start;
	private ParserFactory factory;
	private ParserFactory.Executable code = null;

	public Parser(ParserFactory factory, OProduction start) {
		this.factory = factory;
		this.start = start;
	}

	public final ParserFactory getFactory() {
		return this.factory;
	}

	public final OGrammar getGrammar() {
		if (this.code == null) {
			compile();
		}
		return code.getGrammar();
	}

	public final ParserFactory.Executable compile() {
		long t = factory.nanoTime(null, 0);
		ParserFactory.Compiler compl = this.factory.newCompiler();
		this.code = compl.compile(factory, factory.optimize(factory, start));
		factory.nanoTime("CompilingTime", t);
		return code;
	}

	public final ParserFactory.Executable getExecutable() {
		if (this.code == null) {
			compile();
		}
		return code;
	}

	/* --------------------------------------------------------------------- */

	final <T> T performNull(ParserContext<T> ctx, Source s) {
		ParserFactory.Executable code = this.getExecutable();
		code.initContext(ctx);
		ctx.start();
		T matched = code.exec(ctx);
		ctx.end();
		return matched;
	}

	public final <T> T parse(Source s, TreeConstructor<T> newTree, TreeConnector<T> linkTree) throws IOException {
		ParserContext<T> ctx = this.factory.newContext(s, newTree, linkTree);
		T matched = performNull(ctx, s);
		if (matched == null) {
			perror(s, ctx.getMaximumPosition(), "syntax error");
			return null;
		}
		if (!ctx.eof() && factory.is("partial-failure", this.PartialFailure)) {
			perror(s, ctx.getPosition(), "unconsumed");
		}
		return matched;
	}

	public final <T> long match(Source s, TreeConstructor<T> newTree, TreeConnector<T> linkTree) {
		ParserContext<T> ctx = this.factory.newContext(s, newTree, linkTree);
		T matched = performNull(ctx, s);
		if (matched == null) {
			return -1;
		}
		return ctx.getPosition();
	}

	/* --------------------------------------------------------------------- */

	private static CommonTree defaultTree = new CommonTree();

	public final int match(Source s) {
		return (int) this.match(s, defaultTree, defaultTree);
	}

	public final int match(String str) {
		return match(CommonSource.newStringSource(str));
	}

	public final Tree<?> parse(Source sc) throws IOException {
		return this.parse(sc, defaultTree, defaultTree);
	}

	public final Tree<?> parse(String str) throws IOException {
		return this.parse(CommonSource.newStringSource(str));
	}

	/* Errors */

	private boolean PartialFailure = false;
	private boolean PrintingParserError = false;
	private boolean ThrowingParserError = true;

	public void setPartialFailure(boolean b) {
		this.PartialFailure = b;
	}

	public void setThrowingException(boolean b) {
		this.ThrowingParserError = b;
	}

	public void setPrintingException(boolean b) {
		this.PrintingParserError = b;
	}

	private void perror(Source source, long pos, String message) throws IOException {
		if (PrintingParserError || factory.is("print-parser-error", PrintingParserError)) {
			factory.report(ParserFactory.Error, source.formatPositionLine("error", pos, message));
		}
		if (ThrowingParserError || factory.is("throw-parser-error", ThrowingParserError)) {
			throw new ParserRuntimeException(source, pos, message);
		}
	}

	public static class ParserRuntimeException extends IOException {
		/**
		 * 
		 */
		private static final long serialVersionUID = 6419448216728968762L;
		final Source source;
		final long pos;
		final String message;

		public ParserRuntimeException(Source source, long pos, String message) {
			this.source = source;
			this.pos = pos;
			this.message = message;
		}

		@Override
		public final String toString() {
			return source.formatPositionLine("error", pos, message);
		}
	}

}

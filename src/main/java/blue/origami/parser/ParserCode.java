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

import blue.origami.main.MainOption;
import blue.origami.nez.ast.Source;
import blue.origami.nez.ast.SourcePosition;
import blue.origami.parser.pasm.PAsmAPI.TreeFunc;
import blue.origami.parser.pasm.PAsmAPI.TreeSetFunc;
import blue.origami.util.OFormat;
import blue.origami.util.OOption;

public interface ParserCode {
	public ParserGrammar getParserGrammar();

	public int match(Source s, int pos, TreeFunc newTree, TreeSetFunc linkTree);

	public Object parse(Source s, int pos, TreeFunc newTree, TreeSetFunc linkTree) throws IOException;

	public default void checkSyntaxError(OOption options, Object result) {

	}

	default void perror(OOption options, SourcePosition s, OFormat message) throws IOException {
		if (options.is(MainOption.ThrowingParserError, true)) {
			throw new ParserErrorException(s, message);
		} else {
			options.reportError(s, message);
		}
	}

	default void pwarn(OOption options, SourcePosition s, OFormat message) throws IOException {
		if (options.is(MainOption.ThrowingParserError, true)) {
			throw new ParserErrorException(s, message);
		} else {
			options.reportWarning(s, message);
		}
	}

	@SuppressWarnings("serial")
	public static class ParserErrorException extends IOException {
		final SourcePosition s;
		final OFormat message;

		public ParserErrorException(SourcePosition s, OFormat message) {
			this.s = s;
			this.message = message;
		}

		@Override
		public final String toString() {
			return SourcePosition.formatErrorMessage(this.s, this.message);
		}
	}

}
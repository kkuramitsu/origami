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

import blue.origami.nezcc.ParserGenerator;
import blue.origami.nez.peg.Grammar;
import blue.origami.nezcc.JavaParserGenerator;
import blue.origami.util.OOption;

public class Onezcc extends OCommand {

	// protected void initOption(OOption options) {
	// super.initOption(options);
	// options.set(ParserOption.ThrowingParserError, false);
	// }

	@Override
	public void exec(OOption options) throws Throwable {
		Grammar g = this.getGrammar(options);
		ParserGenerator<StringBuilder, String> pg = options.newInstance(JavaParserGenerator.class);
		pg.generate(g);
	}

}

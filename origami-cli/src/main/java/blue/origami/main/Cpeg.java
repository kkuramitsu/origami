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

import blue.origami.util.ODebug;
import blue.origami.util.OOption;

public class Cpeg extends OCommand {

	@Override
	public void exec(OOption options) throws Throwable {
		ODebug.TODO(this);
		// GrammarWriter grammarWriter =
		// options.newGrammarWriter(origami.main.tool.PEGWriter.class);
		// if (options.is("raw", false)) {
		// grammarWriter.writeGrammar(options, options.getGrammar());
		// } else {
		// Parser p = options.newParser();
		// grammarWriter.writeGrammar(options, p.getGrammar());
		// }
	}

}

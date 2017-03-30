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

import blue.nez.ast.SourcePosition;
import blue.nez.parser.ParserOption;
import blue.nez.peg.Grammar;
import blue.origami.OrigamiContext;
import blue.origami.util.OConsole;
import blue.origami.util.ODebug;
import blue.origami.util.OOption;

public class Ocheck extends Orun {
	@Override
	public void exec(OOption options) throws Throwable {
		int totalTestCount = 0;
		int totalPassCount = 0;
		String[] files = options.list(ParserOption.InputFiles);
		for (String file : files) {
			try {
				String ext = SourcePosition.extractFileExtension(file);
				Grammar g = Grammar.loadFile(ext + ".opeg", options.list(ParserOption.GrammarPath));
				OrigamiContext env = new OrigamiContext(g, options);
				env.loadScriptFile(file);
			} catch (Throwable e) {
				ODebug.traceException(e);
			}
			int testCount = ODebug.getTestCount();
			int passCount = ODebug.getPassCount();
			int failCount = testCount - passCount;
			String msg = (failCount > 0 || testCount == 0) ? OConsole.color(Red, "FAIL") : "OK";
			p(Yellow, "Tested %s (%d/%d) %s", file, passCount, testCount, msg);
			totalTestCount += 1;
			totalPassCount += (failCount > 0 || testCount == 0) ? 0 : 1;
			ODebug.resetCount();
		}
		p(Yellow, "Results (%d/%d)", totalPassCount, totalTestCount);
	}
}

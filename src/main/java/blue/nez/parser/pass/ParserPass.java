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

package blue.nez.parser.pass;

import java.util.ArrayList;

import blue.nez.parser.ParserGrammar;
import blue.nez.parser.ParserOption;
import blue.nez.peg.Rewriter;
import blue.origami.util.ODebug;
import blue.origami.util.OOption;

public abstract class ParserPass extends Rewriter<Void> {
	public abstract ParserGrammar perform(ParserGrammar g, OOption options);

	public final static ParserGrammar applyPass(ParserGrammar g, OOption options) {
		if (options.is(ParserOption.Unoptimized, false)) {
			return g;
		}
		String[] pass = options.stringList(ParserOption.Pass);
		if (pass.length > 0) {
			return applyPass(g, options, loadPassClass(pass, options));
		} else {
			return applyPass(g, options, NotCharPass.class, TreePass.class, DispatchPass.class, InlinePass.class);
		}
	}

	static Class<?>[] loadPassClass(String[] pass, OOption options) {
		ArrayList<Class<?>> l = new ArrayList<>();
		for (String p : pass) {
			try {
				l.add(OOption.loadClass(ParserPass.class, p, options.stringList(ParserOption.PassPath)));
			} catch (ClassNotFoundException e) {
				ODebug.traceException(e);
			}
		}
		return l.toArray(new Class<?>[l.size()]);
	}

	static ParserGrammar applyPass(ParserGrammar g, OOption options, Class<?>... classes) {
		for (Class<?> c : classes) {
			try {
				ParserPass pass = (ParserPass) c.newInstance();
				long t1 = options.nanoTime(null, 0);
				g = pass.perform(g, options);
				options.nanoTime("Pass: " + pass, t1);
			} catch (InstantiationException | IllegalAccessException e) {
				ODebug.traceException(e);
			}
		}
		return g;
	}

}

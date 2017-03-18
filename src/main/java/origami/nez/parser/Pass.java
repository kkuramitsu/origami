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

import origami.nez.peg.Expression;
import origami.nez.peg.Grammar;

public abstract class Pass extends Expression.Rewriter<Void> {
	public abstract void perform(ParserFactory fac, Grammar g);

	public final static void apply(ParserFactory fac, Grammar g, String... passes) {
		for (String p : passes) {
			Pass pass = newInstance(fac, p);
			if (pass != null) {
				long t1 = fac.nanoTime(null, 0);
				pass.perform(fac, g);
				fac.nanoTime("Pass: " + pass.getClass().getSimpleName(), t1);
			}
		}
	}

	public final static void apply(ParserFactory fac, Grammar g, Class<?>... passes) {
		for (Class<?> c : passes) {
			Pass pass = newInstance(fac, c);
			if (pass != null) {
				long t1 = fac.nanoTime(null, 0);
				pass.perform(fac, g);
				fac.nanoTime("Pass: " + pass, t1);
			}
		}
	}

	final static String[] classPrefix = { "", "nez.peg.pass." };

	private static Pass newInstance(ParserFactory fac, Object o) {
		if (o instanceof Pass) {
			return (Pass) o;
		}
		Class<?> c = null;
		if (o instanceof String) {
			for (String path : classPrefix) {
				try {
					c = Class.forName(path + o);
					break;
				} catch (ClassNotFoundException e) {
				}
			}
		}
		if (o instanceof Class<?>) {
			c = (Class<?>) o;
		}
		if (c != null) {
			try {
				return (Pass) c.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				fac.trace(e);
			}
		}
		fac.verbose("undefined pass %s", o);
		return null;
	}

}

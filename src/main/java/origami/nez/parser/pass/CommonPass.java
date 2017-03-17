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

package origami.nez.parser.pass;

import origami.nez.parser.ParserFactory;
import origami.nez.parser.Pass;
import origami.nez.peg.Expression;
import origami.nez.peg.OGrammar;
import origami.nez.peg.OProduction;

class CommonPass extends Pass {
	protected ParserFactory fac;

	@Override
	public void perform(ParserFactory fac, OGrammar g) {
		this.fac = fac;
		prepare(g);
		for (OProduction p : g) {
			g.setExpression(p.getLocalName(), this.rewrite(p.getExpression(), null));
		}
	}

	protected void prepare(OGrammar g) {

	}

	protected Object ref(Expression e) {
		return null;
	}

	private int count = 0;
	private String message = "";

	protected Expression optimized(Expression oldOne, Expression newOne) {
		// if (oldOne instanceof Expression.Pair) {
		// System.out.printf("%s::\n\t%s\n\t%s\n",
		// this.getClass().getSimpleName(), oldOne, newOne);
		// }
		count++;
		return newOne;
	}

	protected Expression debug(Expression oldOne, Expression newOne) {
		System.out.printf("%s::\n\t%s\n\t%s\n", this.getClass().getSimpleName(), oldOne, newOne);
		count++;
		return newOne;
	}

	protected void log(String fmt, Object... a) {
		this.message = "," + String.format(fmt, a);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[" + this.count + this.message + "]";
	}
}

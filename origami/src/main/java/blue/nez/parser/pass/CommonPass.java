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

import blue.nez.parser.ParserPass;
import blue.nez.peg.Expression;
import blue.nez.peg.Grammar;
import blue.nez.peg.Production;
import blue.origami.util.OOption;

class CommonPass extends ParserPass {
	protected OOption options;

	@Override
	public Grammar perform(Grammar g, OOption options) {
		this.options = options;
		this.prepare(g);
		for (Production p : g) {
			g.setExpression(p.getLocalName(), this.rewrite(p.getExpression(), null));
		}
		return g;
	}

	protected void prepare(Grammar g) {

	}

	private int count = 0;
	private String message = "";

	protected Expression optimized(Expression oldOne, Expression newOne) {
		// if (oldOne instanceof Expression.Pair) {
		// System.out.printf("%s::\n\t%s\n\t%s\n",
		// this.getClass().getSimpleName(), oldOne, newOne);
		// }
		this.count++;
		return newOne;
	}

	protected Expression debug(Expression oldOne, Expression newOne) {
		System.out.printf("%s::\n\t%s\n\t%s\n", this.getClass().getSimpleName(), oldOne, newOne);
		this.count++;
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

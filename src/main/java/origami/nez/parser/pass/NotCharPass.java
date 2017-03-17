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

import origami.nez.peg.Expression;
import origami.nez.peg.OGrammar;
import origami.nez.peg.OProduction;

public class NotCharPass extends CommonPass {

	boolean BinaryGrammar = false;

	@Override
	protected void prepare(OGrammar g) {
		for (OProduction p : g.getAllProductions()) {
			checkBinaryExpression(p.getExpression());
		}
	}

	private void checkBinaryExpression(Expression e) {
		if (this.BinaryGrammar) {
			return;
		}
		if (e instanceof Expression.PByte) {
			if (((Expression.PByte) e).byteChar == 0) {
				this.BinaryGrammar = true;
			}
		}
		if (e instanceof Expression.PByteSet) {
			Expression.PByteSet c1 = (Expression.PByteSet) e;
			if (c1.is(0) == true) {
				this.BinaryGrammar = true;
			}
		}
		for (Expression sub : e) {
			checkBinaryExpression(sub);
		}
	}

	@Override
	public Expression visitPair(Expression.PPair e, Void a) {
		Expression p = super.visitPair(e, a);
		if (p instanceof Expression.PPair) {
			Expression nc = NotChar(p.get(0));
			Expression c = Char(p.get(1));
			if (nc != null && c != null) {
				Expression e1 = merge(nc, c);
				return optimized(p, Expression.newSequence(e1, Remain(p.get(1)), ref(p)));
			}
		}
		return p;
	}

	private Expression NotChar(Expression e) {
		if (e instanceof Expression.PNot) {
			Expression nc = Expression.deref(e.get(0));
			if (nc instanceof Expression.PByte || nc instanceof Expression.PByteSet) {
				return nc;
			}
		}
		return null;
	}

	private Expression Char(Expression e) {
		if (e instanceof Expression.PPair) {
			e = e.get(0);
		}
		e = Expression.deref(e);
		if (e instanceof Expression.PAny || e instanceof Expression.PByteSet) {
			return e;
		}
		return null;
	}

	private Expression Remain(Expression e) {
		if (e instanceof Expression.PPair) {
			e = e.get(1);
		}
		return Expression.defaultEmpty;
	}

	private Expression merge(Expression nc, Expression c) {
		Expression.PByteSet bs = new Expression.PByteSet(ref(c));
		if (c instanceof Expression.PAny) {
			bs.set(BinaryGrammar ? 0 : 1, 255, true);
		}
		if (c instanceof Expression.PByteSet) {
			Expression.PByteSet c1 = (Expression.PByteSet) c;
			for (int i = 0; i < 256; i++) {
				if (c1.is(i)) {
					bs.set(i, true);
				}
			}
		}
		if (nc instanceof Expression.PByte) {
			bs.set(((Expression.PByte) nc).byteChar, false);
		}
		if (nc instanceof Expression.PByteSet) {
			Expression.PByteSet c1 = (Expression.PByteSet) nc;
			for (int i = 0; i < 256; i++) {
				if (c1.is(i)) {
					bs.set(i, false);
				}
			}
		}
		return bs;
	}
}

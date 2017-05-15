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

package blue.origami.nez.parser.pass;

import blue.origami.nez.parser.ParserGrammar;
import blue.origami.nez.peg.Expression;
import blue.origami.nez.peg.expression.PAny;
import blue.origami.nez.peg.expression.PByte;
import blue.origami.nez.peg.expression.PByteSet;
import blue.origami.nez.peg.expression.PNot;
import blue.origami.nez.peg.expression.PPair;

public class NotCharPass extends CommonPass {

	boolean BinaryGrammar = false;

	@Override
	protected void prepare(ParserGrammar g) {
		this.BinaryGrammar = g.isBinaryGrammar();
	}

	@Override
	public Expression visitPair(PPair e, Void a) {
		Expression p = super.visitPair(e, a);
		if (p instanceof PPair) {
			Expression nc = this.getNotChar(p.get(0));
			Expression c = this.Char(p.get(1));
			if (nc != null && c != null) {
				Expression e1 = this.merge(nc, c);
				return this.optimized(p, Expression.newSequence(e1, this.Remain(p.get(1))));
			}
		}
		return p;
	}

	private Expression getNotChar(Expression e) {
		if (e instanceof PNot) {
			Expression nc = Expression.deref(e.get(0));
			if (nc instanceof PByte || nc instanceof PByteSet) {
				return nc;
			}
		}
		return null;
	}

	private Expression Char(Expression e) {
		if (e instanceof PPair) {
			e = e.get(0);
		}
		e = Expression.deref(e);
		if (e instanceof PAny || e instanceof PByteSet) {
			return e;
		}
		return null;
	}

	private Expression Remain(Expression e) {
		if (e instanceof PPair) {
			return e.get(1);
		}
		return Expression.defaultEmpty;
	}

	private Expression merge(Expression nc, Expression c) {
		PByteSet bs = new PByteSet();
		if (c instanceof PAny) {
			bs.set(this.BinaryGrammar ? 0 : 1, 255, true);
		}
		if (c instanceof PByteSet) {
			PByteSet c1 = (PByteSet) c;
			for (int i = 0; i < 256; i++) {
				if (c1.is(i)) {
					bs.set(i, true);
				}
			}
		}
		if (nc instanceof PByte) {
			bs.set(((PByte) nc).byteChar(), false);
		}
		if (nc instanceof PByteSet) {
			PByteSet c1 = (PByteSet) nc;
			for (int i = 0; i < 256; i++) {
				if (c1.is(i)) {
					bs.set(i, false);
				}
			}
		}
		return bs;
	}

	// @Override
	// protected Expression optimized(Expression oldOne, Expression newOne) {
	// return this.debug(oldOne, newOne);
	// }

}

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

package blue.nez.peg;

import blue.nez.peg.expression.PAnd;
import blue.nez.peg.expression.PAny;
import blue.nez.peg.expression.PByte;
import blue.nez.peg.expression.PByteSet;
import blue.nez.peg.expression.PChoice;
import blue.nez.peg.expression.PDetree;
import blue.nez.peg.expression.PDispatch;
import blue.nez.peg.expression.PEmpty;
import blue.nez.peg.expression.PFail;
import blue.nez.peg.expression.PIfCondition;
import blue.nez.peg.expression.PLinkTree;
import blue.nez.peg.expression.PNonTerminal;
import blue.nez.peg.expression.PNot;
import blue.nez.peg.expression.POnCondition;
import blue.nez.peg.expression.POption;
import blue.nez.peg.expression.PPair;
import blue.nez.peg.expression.PRepeat;
import blue.nez.peg.expression.PRepetition;
import blue.nez.peg.expression.PReplace;
import blue.nez.peg.expression.PScan;
import blue.nez.peg.expression.PSymbolAction;
import blue.nez.peg.expression.PSymbolPredicate;
import blue.nez.peg.expression.PSymbolScope;
import blue.nez.peg.expression.PTag;
import blue.nez.peg.expression.PTrap;
import blue.nez.peg.expression.PTree;

public enum NonEmpty {
	True, False, Unsure;

	public final static boolean isAlwaysConsumed(Expression e) {
		return minLenFunc.check(e, null) == NonEmpty.True;
	}

	public final static boolean isAlwaysConsumed(Production p) {
		return isConsumedImpl(p.getGrammar(), p.getLocalName(), p.getExpression()) == NonEmpty.True;
	}

	static NonEmpty isConsumedImpl(Grammar g, String name, Expression e) {
		NonEmpty r = g.getProperty(name, NonEmpty.class);
		if (r == null) {
			g.setProperty(name, NonEmpty.Unsure);
			r = minLenFunc.check(e, null);
			g.setProperty(name, r);
		}
		return r;
	}

	private static EmptyCheck minLenFunc = new EmptyCheck();

	private static class EmptyCheck extends ExpressionVisitor<NonEmpty, Void> {
		NonEmpty check(Expression e, Void a) {
			return e.visit(this, a);
		}

		@Override
		public NonEmpty visitNonTerminal(PNonTerminal e, Void a) {
			if (e.getGrammar() == null || e.getProduction() == null) {
				return NonEmpty.False;
			}
			return NonEmpty.isConsumedImpl(e.getGrammar(), e.getLocalName(), e.getExpression());
		}

		@Override
		public NonEmpty visitEmpty(PEmpty e, Void a) {
			return NonEmpty.False;
		}

		@Override
		public NonEmpty visitFail(PFail e, Void a) {
			return NonEmpty.False;
		}

		@Override
		public NonEmpty visitByte(PByte e, Void a) {
			return NonEmpty.True;
		}

		@Override
		public NonEmpty visitByteSet(PByteSet e, Void a) {
			return NonEmpty.True;
		}

		@Override
		public NonEmpty visitAny(PAny e, Void a) {
			return NonEmpty.True;
		}

		@Override
		public NonEmpty visitPair(PPair e, Void a) {
			if (this.check(e.get(0), a) == NonEmpty.True) {
				return NonEmpty.True;
			}
			return this.check(e.get(1), a);
		}

		@Override
		public NonEmpty visitChoice(PChoice e, Void a) {
			boolean unconsumed = false;
			boolean undecided = false;
			for (Expression sub : e) {
				NonEmpty c = this.check(sub, a);
				if (c == NonEmpty.True) {
					continue;
				}
				unconsumed = true;
				if (c == NonEmpty.Unsure) {
					undecided = true;
				}
			}
			if (!unconsumed) {
				return NonEmpty.True;
			}
			return undecided ? NonEmpty.Unsure : NonEmpty.False;
		}

		@Override
		public NonEmpty visitDispatch(PDispatch e, Void a) {
			boolean unconsumed = false;
			boolean undecided = false;
			for (Expression sub : e) {
				NonEmpty c = this.check(sub, a);
				if (c == NonEmpty.True) {
					continue;
				}
				unconsumed = true;
				if (c == NonEmpty.Unsure) {
					undecided = true;
				}
			}
			if (!unconsumed) {
				return NonEmpty.True;
			}
			return undecided ? NonEmpty.Unsure : NonEmpty.False;
		}

		@Override
		public NonEmpty visitOption(POption e, Void a) {
			return NonEmpty.False;
		}

		@Override
		public NonEmpty visitRepetition(PRepetition e, Void a) {
			if (e.isOneMore()) {
				return this.check(e.get(0), a);
			}
			return NonEmpty.False;
		}

		@Override
		public NonEmpty visitAnd(PAnd e, Void a) {
			return NonEmpty.False;
		}

		@Override
		public NonEmpty visitNot(PNot e, Void a) {
			return NonEmpty.False;
		}

		@Override
		public NonEmpty visitTree(PTree e, Void a) {
			return this.check(e.get(0), a);
		}

		@Override
		public NonEmpty visitLinkTree(PLinkTree e, Void a) {
			return this.check(e.get(0), a);
		}

		@Override
		public NonEmpty visitTag(PTag e, Void a) {
			return NonEmpty.False;
		}

		@Override
		public NonEmpty visitReplace(PReplace e, Void a) {
			return NonEmpty.False;
		}

		@Override
		public NonEmpty visitDetree(PDetree e, Void a) {
			return this.check(e.get(0), a);
		}

		@Override
		public NonEmpty visitSymbolScope(PSymbolScope e, Void a) {
			return this.check(e.get(0), a);
		}

		@Override
		public NonEmpty visitSymbolAction(PSymbolAction e, Void a) {
			return this.check(e.get(0), a);
		}

		@Override
		public NonEmpty visitSymbolPredicate(PSymbolPredicate e, Void a) {
			if (e.funcName == NezFunc.exists) {
				return NonEmpty.Unsure;
			}
			return this.check(e.get(0), a);
		}

		@Override
		public NonEmpty visitScan(PScan e, Void a) {
			return this.check(e.get(0), a);
		}

		@Override
		public NonEmpty visitRepeat(PRepeat e, Void a) {
			/* There is a case where we repeat 0 times */
			return NonEmpty.False;
		}

		@Override
		public NonEmpty visitIf(PIfCondition e, Void a) {
			return NonEmpty.False;
		}

		@Override
		public NonEmpty visitOn(POnCondition e, Void a) {
			return this.check(e.get(0), a);
		}

		@Override
		public NonEmpty visitTrap(PTrap e, Void a) {
			return NonEmpty.False;
		}
	}

}

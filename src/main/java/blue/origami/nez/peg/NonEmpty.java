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

package blue.origami.nez.peg;

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
		public NonEmpty visitNonTerminal(Expression.PNonTerminal e, Void a) {
			return NonEmpty.isConsumedImpl(e.getGrammar(), e.getLocalName(), e.getExpression());
		}

		@Override
		public NonEmpty visitEmpty(Expression.PEmpty e, Void a) {
			return NonEmpty.False;
		}

		@Override
		public NonEmpty visitFail(Expression.PFail e, Void a) {
			return NonEmpty.False;
		}

		@Override
		public NonEmpty visitByte(Expression.PByte e, Void a) {
			return NonEmpty.True;
		}

		@Override
		public NonEmpty visitByteSet(Expression.PByteSet e, Void a) {
			return NonEmpty.True;
		}

		@Override
		public NonEmpty visitAny(Expression.PAny e, Void a) {
			return NonEmpty.True;
		}

		@Override
		public NonEmpty visitPair(Expression.PPair e, Void a) {
			if (check(e.get(0), a) == NonEmpty.True) {
				return NonEmpty.True;
			}
			return check(e.get(1), a);
		}

		@Override
		public NonEmpty visitChoice(Expression.PChoice e, Void a) {
			boolean unconsumed = false;
			boolean undecided = false;
			for (Expression sub : e) {
				NonEmpty c = check(sub, a);
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
		public NonEmpty visitDispatch(Expression.PDispatch e, Void a) {
			boolean unconsumed = false;
			boolean undecided = false;
			for (Expression sub : e) {
				NonEmpty c = check(sub, a);
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
		public NonEmpty visitOption(Expression.POption e, Void a) {
			return NonEmpty.False;
		}

		@Override
		public NonEmpty visitRepetition(Expression.PRepetition e, Void a) {
			if (e.isOneMore()) {
				return check(e.get(0), a);
			}
			return NonEmpty.False;
		}

		@Override
		public NonEmpty visitAnd(Expression.PAnd e, Void a) {
			return NonEmpty.False;
		}

		@Override
		public NonEmpty visitNot(Expression.PNot e, Void a) {
			return NonEmpty.False;
		}

		@Override
		public NonEmpty visitTree(Expression.PTree e, Void a) {
			return check(e.get(0), a);
		}

		@Override
		public NonEmpty visitLinkTree(Expression.PLinkTree e, Void a) {
			return check(e.get(0), a);
		}

		@Override
		public NonEmpty visitTag(Expression.PTag e, Void a) {
			return NonEmpty.False;
		}

		@Override
		public NonEmpty visitReplace(Expression.PReplace e, Void a) {
			return NonEmpty.False;
		}

		@Override
		public NonEmpty visitDetree(Expression.PDetree e, Void a) {
			return check(e.get(0), a);
		}

		@Override
		public NonEmpty visitSymbolScope(Expression.PSymbolScope e, Void a) {
			return check(e.get(0), a);
		}

		@Override
		public NonEmpty visitSymbolAction(Expression.PSymbolAction e, Void a) {
			return check(e.get(0), a);
		}

		@Override
		public NonEmpty visitSymbolPredicate(Expression.PSymbolPredicate e, Void a) {
			if (e.funcName == NezFunc.exists) {
				return NonEmpty.Unsure;
			}
			return check(e.get(0), a);
		}

		@Override
		public NonEmpty visitScan(Expression.PScan e, Void a) {
			return check(e.get(0), a);
		}

		@Override
		public NonEmpty visitRepeat(Expression.PRepeat e, Void a) {
			/* There is a case where we repeat 0 times */
			return NonEmpty.False;
		}

		@Override
		public NonEmpty visitIf(Expression.PIfCondition e, Void a) {
			return NonEmpty.False;
		}

		@Override
		public NonEmpty visitOn(Expression.POnCondition e, Void a) {
			return check(e.get(0), a);
		}

		@Override
		public NonEmpty visitTrap(Expression.PTrap e, Void a) {
			return NonEmpty.False;
		}
	}

}

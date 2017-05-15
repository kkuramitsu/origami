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

import blue.origami.nez.peg.expression.PAnd;
import blue.origami.nez.peg.expression.PAny;
import blue.origami.nez.peg.expression.PByte;
import blue.origami.nez.peg.expression.PByteSet;
import blue.origami.nez.peg.expression.PChoice;
import blue.origami.nez.peg.expression.PDetree;
import blue.origami.nez.peg.expression.PDispatch;
import blue.origami.nez.peg.expression.PEmpty;
import blue.origami.nez.peg.expression.PFail;
import blue.origami.nez.peg.expression.PIf;
import blue.origami.nez.peg.expression.PLinkTree;
import blue.origami.nez.peg.expression.PMany;
import blue.origami.nez.peg.expression.PNonTerminal;
import blue.origami.nez.peg.expression.PNot;
import blue.origami.nez.peg.expression.POn;
import blue.origami.nez.peg.expression.POption;
import blue.origami.nez.peg.expression.PPair;
import blue.origami.nez.peg.expression.PSymbolAction;
import blue.origami.nez.peg.expression.PSymbolPredicate;
import blue.origami.nez.peg.expression.PSymbolScope;
import blue.origami.nez.peg.expression.PTag;
import blue.origami.nez.peg.expression.PTrap;
import blue.origami.nez.peg.expression.PTree;
import blue.origami.nez.peg.expression.PValue;

public enum Stateful {
	True, False, Unsure;

	public final static boolean isStateful(Expression e) {
		return checkFunc.check(e, null) != Stateful.False;
	}

	public final static boolean isStateful(Production p) {
		return compute(p.getGrammar(), p.getLocalName(), p.getExpression()) != Stateful.False;
	}

	public static Stateful compute(Grammar g, String name, Expression e) {
		Stateful r = g.getProperty(name, Stateful.class);
		if (r == null) {
			g.setProperty(name, Stateful.Unsure);
			r = checkFunc.check(e, null);
			g.setProperty(name, r);
		}
		return r;
	}

	private static StateCheck checkFunc = new StateCheck();

	private static class StateCheck extends ExpressionVisitor<Stateful, Void> {
		Stateful check(Expression e, Void a) {
			return e.visit(this, a);
		}

		@Override
		public Stateful visitNonTerminal(PNonTerminal e, Void a) {
			if (e.getGrammar() == null || e.getProduction() == null) {
				return Stateful.False;
			}
			return Stateful.compute(e.getGrammar(), e.getLocalName(), e.getExpression());
		}

		@Override
		public Stateful visitEmpty(PEmpty e, Void a) {
			return Stateful.False;
		}

		@Override
		public Stateful visitFail(PFail e, Void a) {
			return Stateful.False;
		}

		@Override
		public Stateful visitByte(PByte e, Void a) {
			return Stateful.False;
		}

		@Override
		public Stateful visitByteSet(PByteSet e, Void a) {
			return Stateful.False;
		}

		@Override
		public Stateful visitAny(PAny e, Void a) {
			return Stateful.False;
		}

		@Override
		public Stateful visitPair(PPair e, Void a) {
			if (this.check(e.get(0), a) == Stateful.True) {
				return Stateful.True;
			}
			return this.check(e.get(1), a);
		}

		@Override
		public Stateful visitChoice(PChoice e, Void a) {
			boolean undecided = false;
			for (Expression sub : e) {
				Stateful c = this.check(sub, a);
				if (c == Stateful.True) {
					return c;
				}
				if (c == Stateful.Unsure) {
					undecided = true;
				}
			}
			return undecided ? Stateful.Unsure : Stateful.False;
		}

		@Override
		public Stateful visitDispatch(PDispatch e, Void a) {
			boolean undecided = false;
			for (Expression sub : e) {
				Stateful c = this.check(sub, a);
				if (c == Stateful.True) {
					return c;
				}
				if (c == Stateful.Unsure) {
					undecided = true;
				}
			}
			return undecided ? Stateful.Unsure : Stateful.False;
		}

		@Override
		public Stateful visitOption(POption e, Void a) {
			return this.check(e.get(0), a);
		}

		@Override
		public Stateful visitMany(PMany e, Void a) {
			return this.check(e.get(0), a);
		}

		@Override
		public Stateful visitAnd(PAnd e, Void a) {
			return this.check(e.get(0), a);
		}

		@Override
		public Stateful visitNot(PNot e, Void a) {
			return Stateful.False;
		}

		@Override
		public Stateful visitTree(PTree e, Void a) {
			return this.check(e.get(0), a);
		}

		@Override
		public Stateful visitLinkTree(PLinkTree e, Void a) {
			return this.check(e.get(0), a);
		}

		@Override
		public Stateful visitTag(PTag e, Void a) {
			return Stateful.False;
		}

		@Override
		public Stateful visitValue(PValue e, Void a) {
			return Stateful.False;
		}

		@Override
		public Stateful visitDetree(PDetree e, Void a) {
			return this.check(e.get(0), a);
		}

		@Override
		public Stateful visitSymbolScope(PSymbolScope e, Void a) {
			return this.check(e.get(0), a);
		}

		@Override
		public Stateful visitSymbolAction(PSymbolAction e, Void a) {
			return this.check(e.get(0), a);
		}

		@Override
		public Stateful visitSymbolPredicate(PSymbolPredicate e, Void a) {
			return Stateful.True;
		}

		@Override
		public Stateful visitIf(PIf e, Void a) {
			return Stateful.True;
		}

		@Override
		public Stateful visitOn(POn e, Void a) {
			return this.check(e.get(0), a);
		}

		@Override
		public Stateful visitTrap(PTrap e, Void a) {
			return Stateful.False;
		}
	}

}

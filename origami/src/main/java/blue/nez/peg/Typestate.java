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
import blue.nez.peg.expression.PValue;
import blue.nez.peg.expression.PScan;
import blue.nez.peg.expression.PSymbolAction;
import blue.nez.peg.expression.PSymbolPredicate;
import blue.nez.peg.expression.PSymbolScope;
import blue.nez.peg.expression.PTag;
import blue.nez.peg.expression.PTrap;
import blue.nez.peg.expression.PTree;

public enum Typestate {
	Unit, Tree, Fold, TreeMutation, Immutation, Undecided;

	static TypestateAnalyzer analyzer = new TypestateAnalyzer();

	public static final Typestate compute(Production p) {
		return compute(p.getGrammar(), p.getLocalName(), p.getExpression());
	}

	public static final Typestate compute(Expression e) {
		if (e instanceof PNonTerminal) {
			return compute(((PNonTerminal) e).getProduction());
		}
		return analyzer.compute(e, null);
	}

	public static final Typestate compute(Grammar g, String name, Expression e) {
		Typestate v = g.getProperty(name, Typestate.class);
		if (v == null) {
			g.setProperty(name, Typestate.Undecided);
			v = compute(e);
			if (Typestate.Undecided != v) {
				g.setProperty(name, v);
			}
			return v;
		}
		return (v instanceof Typestate) ? (Typestate) v : Typestate.Undecided;
	}

	final static class TypestateAnalyzer extends ExpressionVisitor<Typestate, Void> {

		public Typestate compute(Expression e, Void memo) {
			return e.visit(this, null);
		}

		@Override
		public Typestate visitNonTerminal(PNonTerminal e, Void memo) {
			if (e.getGrammar() == null || e.getProduction() == null) {
				return Typestate.Unit;
			}
			return Typestate.compute(e.getProduction());
		}

		@Override
		public Typestate visitEmpty(PEmpty e, Void memo) {
			return Typestate.Unit;
		}

		@Override
		public Typestate visitFail(PFail e, Void memo) {
			return Typestate.Unit;
		}

		@Override
		public Typestate visitByte(PByte e, Void memo) {
			return Typestate.Unit;
		}

		@Override
		public Typestate visitByteSet(PByteSet e, Void memo) {
			return Typestate.Unit;
		}

		@Override
		public Typestate visitAny(PAny e, Void memo) {
			return Typestate.Unit;
		}

		@Override
		public Typestate visitPair(PPair e, Void memo) {
			Typestate ts = this.compute(e.get(0), memo);
			if (ts == Typestate.Unit) {
				return this.compute(e.get(1), memo);
			}
			return ts;
		}

		@Override
		public Typestate visitChoice(PChoice e, Void memo) {
			Typestate t = this.compute(e.get(0), memo);
			if (t == Typestate.Tree || t == Typestate.Fold || t == Typestate.TreeMutation) {
				return t;
			}
			for (int i = 1; i < e.size(); i++) {
				t = this.compute(e.get(i), memo);
				if (t == Typestate.Tree || t == Typestate.Fold || t == Typestate.TreeMutation) {
					return t;
				}
			}
			return t;
		}

		@Override
		public Typestate visitDispatch(PDispatch e, Void memo) {
			Typestate t = Typestate.Unit;
			for (int i = 1; i < e.size(); i++) {
				t = this.compute(e.get(i), memo);
				if (t == Typestate.Tree || t == Typestate.Fold || t == Typestate.TreeMutation) {
					return t;
				}
			}
			return t;
		}

		@Override
		public Typestate visitOption(POption e, Void memo) {
			Typestate ts = this.compute(e.get(0), memo);
			if (ts == Typestate.Tree) {
				return Typestate.TreeMutation;
			}
			return ts;
		}

		@Override
		public Typestate visitRepetition(PRepetition e, Void memo) {
			Typestate ts = this.compute(e.get(0), memo);
			if (ts == Typestate.Tree) {
				return Typestate.TreeMutation;
			}
			return ts;
		}

		@Override
		public Typestate visitAnd(PAnd e, Void memo) {
			Typestate ts = this.compute(e.get(0), memo);
			if (ts == Typestate.Tree) {
				return Typestate.TreeMutation;
			}
			return ts;
		}

		@Override
		public Typestate visitNot(PNot e, Void memo) {
			return Typestate.Unit;
		}

		@Override
		public Typestate visitTree(PTree e, Void memo) {
			return e.folding ? Typestate.Fold : Typestate.Tree;
		}

		@Override
		public Typestate visitLinkTree(PLinkTree e, Void memo) {
			return Typestate.TreeMutation;
		}

		@Override
		public Typestate visitTag(PTag e, Void memo) {
			return Typestate.TreeMutation;
		}

		@Override
		public Typestate visitReplace(PValue e, Void memo) {
			return Typestate.TreeMutation;
		}

		@Override
		public Typestate visitDetree(PDetree e, Void memo) {
			return Typestate.Unit;
		}

		@Override
		public final Typestate visitSymbolScope(PSymbolScope e, Void memo) {
			return this.compute(e.get(0), memo);
		}

		@Override
		public final Typestate visitSymbolAction(PSymbolAction e, Void memo) {
			return this.compute(e.get(0), memo);
		}

		@Override
		public final Typestate visitSymbolPredicate(PSymbolPredicate e, Void memo) {
			return Typestate.Unit;
		}

		@Override
		public final Typestate visitScan(PScan e, Void memo) {
			return this.compute(e.get(0), memo);
		}

		@Override
		public final Typestate visitRepeat(PRepeat e, Void memo) {
			Typestate ts = this.compute(e.get(0), memo);
			if (ts == Typestate.Tree) {
				return Typestate.TreeMutation;
			}
			return ts;
		}

		@Override
		public final Typestate visitIf(PIfCondition e, Void memo) {
			return Typestate.Unit;
		}

		@Override
		public final Typestate visitOn(POnCondition e, Void memo) {
			return this.compute(e.get(0), memo);
		}

		@Override
		public Typestate visitTrap(PTrap e, Void memo) {
			return Typestate.Unit;
		}

	}
}

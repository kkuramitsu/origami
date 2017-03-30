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

import blue.origami.nez.peg.Expression.PNonTerminal;

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
		public Typestate visitNonTerminal(Expression.PNonTerminal e, Void memo) {
			return Typestate.compute(e.getProduction());
		}

		@Override
		public Typestate visitEmpty(Expression.PEmpty e, Void memo) {
			return Typestate.Unit;
		}

		@Override
		public Typestate visitFail(Expression.PFail e, Void memo) {
			return Typestate.Unit;
		}

		@Override
		public Typestate visitByte(Expression.PByte e, Void memo) {
			return Typestate.Unit;
		}

		@Override
		public Typestate visitByteSet(Expression.PByteSet e, Void memo) {
			return Typestate.Unit;
		}

		@Override
		public Typestate visitAny(Expression.PAny e, Void memo) {
			return Typestate.Unit;
		}

		@Override
		public Typestate visitPair(Expression.PPair e, Void memo) {
			Typestate ts = compute(e.get(0), memo);
			if (ts == Typestate.Unit) {
				return compute(e.get(1), memo);
			}
			return ts;
		}

		@Override
		public Typestate visitChoice(Expression.PChoice e, Void memo) {
			Typestate t = compute(e.get(0), memo);
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
		public Typestate visitDispatch(Expression.PDispatch e, Void memo) {
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
		public Typestate visitOption(Expression.POption e, Void memo) {
			Typestate ts = this.compute(e.get(0), memo);
			if (ts == Typestate.Tree) {
				return Typestate.TreeMutation;
			}
			return ts;
		}

		@Override
		public Typestate visitRepetition(Expression.PRepetition e, Void memo) {
			Typestate ts = this.compute(e.get(0), memo);
			if (ts == Typestate.Tree) {
				return Typestate.TreeMutation;
			}
			return ts;
		}

		@Override
		public Typestate visitAnd(Expression.PAnd e, Void memo) {
			Typestate ts = this.compute(e.get(0), memo);
			if (ts == Typestate.Tree) {
				return Typestate.TreeMutation;
			}
			return ts;
		}

		@Override
		public Typestate visitNot(Expression.PNot e, Void memo) {
			return Typestate.Unit;
		}

		@Override
		public Typestate visitTree(Expression.PTree e, Void memo) {
			return e.folding ? Typestate.Fold : Typestate.Tree;
		}

		@Override
		public Typestate visitLinkTree(Expression.PLinkTree e, Void memo) {
			return Typestate.TreeMutation;
		}

		@Override
		public Typestate visitTag(Expression.PTag e, Void memo) {
			return Typestate.TreeMutation;
		}

		@Override
		public Typestate visitReplace(Expression.PReplace e, Void memo) {
			return Typestate.TreeMutation;
		}

		@Override
		public Typestate visitDetree(Expression.PDetree e, Void memo) {
			return Typestate.Unit;
		}

		@Override
		public final Typestate visitSymbolScope(Expression.PSymbolScope e, Void memo) {
			return this.compute(e.get(0), memo);
		}

		@Override
		public final Typestate visitSymbolAction(Expression.PSymbolAction e, Void memo) {
			return this.compute(e.get(0), memo);
		}

		@Override
		public final Typestate visitSymbolPredicate(Expression.PSymbolPredicate e, Void memo) {
			return Typestate.Unit;
		}

		@Override
		public final Typestate visitScan(Expression.PScan e, Void memo) {
			return this.compute(e.get(0), memo);
		}

		@Override
		public final Typestate visitRepeat(Expression.PRepeat e, Void memo) {
			Typestate ts = this.compute(e.get(0), memo);
			if (ts == Typestate.Tree) {
				return Typestate.TreeMutation;
			}
			return ts;
		}

		@Override
		public final Typestate visitIf(Expression.PIfCondition e, Void memo) {
			return Typestate.Unit;
		}

		@Override
		public final Typestate visitOn(Expression.POnCondition e, Void memo) {
			return this.compute(e.get(0), memo);
		}

		@Override
		public Typestate visitTrap(Expression.PTrap e, Void memo) {
			return Typestate.Unit;
		}

	}
}

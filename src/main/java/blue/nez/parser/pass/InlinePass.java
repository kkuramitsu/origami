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

import java.util.ArrayList;
import java.util.HashMap;

import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;
import blue.nez.peg.Grammar;
import blue.nez.peg.Production;
import blue.nez.peg.expression.PAnd;
import blue.nez.peg.expression.PAny;
import blue.nez.peg.expression.PByte;
import blue.nez.peg.expression.PByteSet;
import blue.nez.peg.expression.PChoice;
import blue.nez.peg.expression.PDetree;
import blue.nez.peg.expression.PDispatch;
import blue.nez.peg.expression.PEmpty;
import blue.nez.peg.expression.PFail;
import blue.nez.peg.expression.PIf;
import blue.nez.peg.expression.PLinkTree;
import blue.nez.peg.expression.PMany;
import blue.nez.peg.expression.PNonTerminal;
import blue.nez.peg.expression.PNot;
import blue.nez.peg.expression.POn;
import blue.nez.peg.expression.POption;
import blue.nez.peg.expression.PPair;
import blue.nez.peg.expression.PSymbolAction;
import blue.nez.peg.expression.PSymbolPredicate;
import blue.nez.peg.expression.PSymbolScope;
import blue.nez.peg.expression.PTag;
import blue.nez.peg.expression.PTrap;
import blue.nez.peg.expression.PTree;
import blue.nez.peg.expression.PValue;
import blue.origami.util.OOption;

public class InlinePass extends CommonPass {

	HashMap<String, Integer> countMap = new HashMap<>();

	@Override
	protected void prepare(Grammar g) {
		Production start = g.getStartProduction();
		this.countMap.put(start.getUniqueName(), 1);
		this.count(start.getExpression());
	}

	private void count(Expression e) {
		if (e instanceof PNonTerminal) {
			String uname = ((PNonTerminal) e).getUniqueName();
			Integer n = this.countMap.get(uname);
			if (n == null) {
				this.countMap.put(uname, 1);
				this.count(((PNonTerminal) e).getProduction().getExpression());
			} else {
				this.countMap.put(uname, n + 1);
			}
		}
		for (Expression sub : e) {
			this.count(sub);
		}
	}

	@Override
	public Grammar perform(Grammar g, OOption options) {
		this.options = options;
		this.prepare(g);
		for (Production p : g) {
			if (this.countMap.get(p.getLocalName()) != null) {
				g.setExpression(p.getLocalName(), this.rewrite(p.getExpression(), null));
			}
		}
		this.countMap.clear();
		this.prepare(g);
		ArrayList<Production> l = new ArrayList<>(g.size());
		for (Production p : g) {
			Integer c = this.countMap.get(p.getLocalName());
			if (c != null) {
				// System.out.println("remain " + c + ", " + p);
				l.add(p);
			}
		}
		// System.out.printf("inlining %d => %d\n", g.size(), l.size());
		this.log("inlining %d => %d", g.size(), l.size());
		g.replaceAll(l);
		return g;
	}

	@Override
	public Expression visitNonTerminal(PNonTerminal e, Void a) {
		Expression p = e.getProduction().getExpression();
		Expression deref = Expression.deref(p);
		Integer c = this.countMap.get(e.getUniqueName());
		if (c == 1) {
			return this.optimized(e, deref);
		}
		// if (e.getLocalName().isTerminal()) {
		// return optimized(e, deref);
		// }
		// System.out.println("@@ " + nz86.count(deref) + " " + deref);
		if (analysis.cost(deref, 0) <= 2) {
			// System.out
			// .println("remain " + c + ", " + analysis.cost(deref, 0) + ", " +
			// e.getUniqueName() + " = " + deref);
			return this.optimized(e, deref);
		}
		return e;
	}

	private static CostAnalysis analysis = new CostAnalysis();

	private static class CostAnalysis extends ExpressionVisitor<Integer, Integer> {

		int cost(Expression e, int calls) {
			if (calls > 4) {
				return calls;
			}
			return e.visit(this, calls);
		}

		@Override
		public Integer visitNonTerminal(PNonTerminal e, Integer calls) {
			return calls + 1;
		}

		@Override
		public Integer visitEmpty(PEmpty e, Integer calls) {
			return calls;
		}

		@Override
		public Integer visitFail(PFail e, Integer calls) {
			return calls;
		}

		@Override
		public Integer visitByte(PByte e, Integer calls) {
			return calls + 1;
		}

		@Override
		public Integer visitByteSet(PByteSet e, Integer calls) {
			return calls + 1;
		}

		@Override
		public Integer visitAny(PAny e, Integer calls) {
			return calls + 1;
		}

		@Override
		public Integer visitPair(PPair e, Integer calls) {
			// if (e.get(0) instanceof PByte) {
			// Expression remaining = Expression.extractMultiBytes(e, null);
			// return remaining.visit(this, calls + 1);
			// }
			return this.cost(e.get(1), this.cost(e.get(0), calls));
		}

		@Override
		public Integer visitChoice(PChoice e, Integer calls) {
			for (Expression sub : e) {
				calls = this.cost(sub, calls + 1);
			}
			return calls;
		}

		@Override
		public Integer visitDispatch(PDispatch e, Integer calls) {
			for (Expression sub : e) {
				calls = this.cost(sub, calls);
			}
			return calls;
		}

		@Override
		public Integer visitOption(POption e, Integer calls) {
			Expression deref = Expression.deref(e.get(0));
			if (deref instanceof PByte || deref instanceof PByteSet) {
				return calls + 1;
			}
			return this.cost(e.get(0), calls + 1);
		}

		@Override
		public Integer visitMany(PMany e, Integer calls) {
			Expression deref = Expression.deref(e.get(0));
			if (deref instanceof PByte || deref instanceof PByteSet) {
				return calls + 1;
			}
			return this.cost(e.get(0), calls + 1);
		}

		@Override
		public Integer visitAnd(PAnd e, Integer calls) {
			Expression deref = Expression.deref(e.get(0));
			if (deref instanceof PByte || deref instanceof PByteSet) {
				return calls + 1;
			}
			return this.cost(e.get(0), calls + 1);
		}

		@Override
		public Integer visitNot(PNot e, Integer calls) {
			Expression deref = Expression.deref(e.get(0));
			if (deref instanceof PByte || deref instanceof PByteSet) {
				return calls + 1;
			}
			return this.cost(e.get(0), calls + 1);
		}

		@Override
		public Integer visitTree(PTree e, Integer calls) {
			return this.cost(e.get(0), calls + 3);
		}

		@Override
		public Integer visitDetree(PDetree e, Integer calls) {
			return this.cost(e.get(0), calls + 2);
		}

		@Override
		public Integer visitLinkTree(PLinkTree e, Integer calls) {
			return this.cost(e.get(0), calls + 2);
		}

		@Override
		public Integer visitTag(PTag e, Integer calls) {
			return calls + 1;
		}

		@Override
		public Integer visitValue(PValue e, Integer calls) {
			return calls + 1;
		}

		@Override
		public Integer visitSymbolScope(PSymbolScope e, Integer calls) {
			return this.cost(e.get(0), calls + 2);
		}

		@Override
		public Integer visitSymbolAction(PSymbolAction e, Integer calls) {
			return this.cost(e.get(0), calls + 2);
		}

		@Override
		public Integer visitSymbolPredicate(PSymbolPredicate e, Integer calls) {
			return this.cost(e.get(0), calls + 2);
		}

		@Override
		public Integer visitIf(PIf e, Integer calls) {
			return calls;
		}

		@Override
		public Integer visitOn(POn e, Integer calls) {
			return calls;
		}

		@Override
		public Integer visitTrap(PTrap e, Integer calls) {
			return calls + 1;
		}

	}

}

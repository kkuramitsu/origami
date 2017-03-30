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
import blue.nez.peg.Expression.PAnd;
import blue.nez.peg.Expression.PAny;
import blue.nez.peg.Expression.PByte;
import blue.nez.peg.Expression.PByteSet;
import blue.nez.peg.Expression.PChoice;
import blue.nez.peg.Expression.PDetree;
import blue.nez.peg.Expression.PDispatch;
import blue.nez.peg.Expression.PEmpty;
import blue.nez.peg.Expression.PFail;
import blue.nez.peg.Expression.PIfCondition;
import blue.nez.peg.Expression.PLinkTree;
import blue.nez.peg.Expression.PNonTerminal;
import blue.nez.peg.Expression.PNot;
import blue.nez.peg.Expression.POnCondition;
import blue.nez.peg.Expression.POption;
import blue.nez.peg.Expression.PPair;
import blue.nez.peg.Expression.PRepeat;
import blue.nez.peg.Expression.PRepetition;
import blue.nez.peg.Expression.PReplace;
import blue.nez.peg.Expression.PScan;
import blue.nez.peg.Expression.PSymbolAction;
import blue.nez.peg.Expression.PSymbolPredicate;
import blue.nez.peg.Expression.PSymbolScope;
import blue.nez.peg.Expression.PTag;
import blue.nez.peg.Expression.PTrap;
import blue.nez.peg.Expression.PTree;
import blue.origami.util.OOption;

public class InlinePass extends CommonPass {

	HashMap<String, Integer> countMap = new HashMap<>();

	@Override
	protected void prepare(Grammar g) {
		Production start = g.getStartProduction();
		countMap.put(start.getUniqueName(), 1);
		count(start.getExpression());
	}

	private void count(Expression e) {
		if (e instanceof Expression.PNonTerminal) {
			String uname = ((Expression.PNonTerminal) e).getUniqueName();
			Integer n = countMap.get(uname);
			if (n == null) {
				countMap.put(uname, 1);
				count(((Expression.PNonTerminal) e).getProduction().getExpression());
			} else {
				countMap.put(uname, n + 1);
			}
		}
		for (Expression sub : e) {
			count(sub);
		}
	}

	@Override
	public Grammar perform(Grammar g, OOption options) {
		this.options = options;
		prepare(g);
		for (Production p : g) {
			if (countMap.get(p.getLocalName()) != null) {
				g.setExpression(p.getLocalName(), this.rewrite(p.getExpression(), null));
			}
		}
		countMap.clear();
		prepare(g);
		ArrayList<Production> l = new ArrayList<>(g.size());
		for (Production p : g) {
			if (countMap.get(p.getLocalName()) != null) {
				l.add(p);
			}
		}
		g.replaceAll(l);
		log("inlining %d => %d", g.size(), l.size());
		return g;
	}

	@Override
	public Expression visitNonTerminal(Expression.PNonTerminal e, Void a) {
		Expression deref = Expression.deref(e.getProduction().getExpression());
		Integer c = countMap.get(e.getUniqueName());
		if (c == 1) {
			return optimized(e, deref);
		}
		// if (e.getLocalName().isTerminal()) {
		// return optimized(e, deref);
		// }
		// System.out.println("@@ " + nz86.count(deref) + " " + deref);
		if (nz86.count(deref) < 2) {
			return optimized(e, deref);
		}
		return e;
	}

	private static NZ86Counter nz86 = new NZ86Counter();

	private static class NZ86Counter extends ExpressionVisitor<Integer, Integer> {

		int count(Expression e) {
			return e.visit(this, 0);
		}

		private Integer step(Expression e, int n) {
			return e.get(n).visit(this, 0);
		}

		@Override
		public Integer visitNonTerminal(PNonTerminal e, Integer step) {
			return step + 1;
		}

		@Override
		public Integer visitEmpty(PEmpty e, Integer step) {
			return step;
		}

		@Override
		public Integer visitFail(PFail e, Integer step) {
			return step + 1;
		}

		@Override
		public Integer visitByte(PByte e, Integer step) {
			return step + 1;
		}

		@Override
		public Integer visitByteSet(PByteSet e, Integer step) {
			return step + 1;
		}

		@Override
		public Integer visitAny(PAny e, Integer step) {
			return step + 1;
		}

		@Override
		public Integer visitPair(PPair e, Integer step) {
			if (e.get(0) instanceof Expression.PByte) {
				Expression remaining = Expression.extractString(e, null);
				return remaining.visit(this, step + 1);
			}
			return step + step(e, 0) + step(e, 1);
		}

		@Override
		public Integer visitChoice(PChoice e, Integer step) {
			int sum = step;
			for (Expression sub : e) {
				sum += sub.visit(this, 2);
			}
			return sum;
		}

		@Override
		public Integer visitDispatch(PDispatch e, Integer step) {
			int sum = step;
			for (Expression sub : e) {
				sum += sub.visit(this, 2);
			}
			return sum;
		}

		@Override
		public Integer visitOption(POption e, Integer step) {
			Expression deref = Expression.deref(e.get(0));
			Integer in = deref.visit(this, 0);
			return (in == 1) ? step + 1 : step + in + 2;
		}

		@Override
		public Integer visitRepetition(PRepetition e, Integer step) {
			Expression deref = Expression.deref(e.get(0));
			Integer in = deref.visit(this, 0);
			return (in == 1) ? step + 1 : step + in + 2;
		}

		@Override
		public Integer visitAnd(PAnd e, Integer step) {
			return step(e, 0) + step + 2;
		}

		@Override
		public Integer visitNot(PNot e, Integer step) {
			Expression deref = Expression.deref(e.get(0));
			Integer in = deref.visit(this, 0);
			return (in == 1) ? step + 1 : step + in + 2;
		}

		@Override
		public Integer visitTree(PTree e, Integer step) {
			return step + step(e, 0) + 2;
		}

		@Override
		public Integer visitDetree(PDetree e, Integer step) {
			return step + step(e, 0) + 2;
		}

		@Override
		public Integer visitLinkTree(PLinkTree e, Integer step) {
			return step + step(e, 0) + 2;
		}

		@Override
		public Integer visitTag(PTag e, Integer step) {
			return step + 1;
		}

		@Override
		public Integer visitReplace(PReplace e, Integer step) {
			return step + 1;
		}

		@Override
		public Integer visitSymbolScope(PSymbolScope e, Integer step) {
			return step + step(e, 0) + 2;
		}

		@Override
		public Integer visitSymbolAction(PSymbolAction e, Integer step) {
			return step + step(e, 0) + 2;
		}

		@Override
		public Integer visitSymbolPredicate(PSymbolPredicate e, Integer step) {
			return step + step(e, 0) + 2;
		}

		@Override
		public Integer visitIf(PIfCondition e, Integer step) {
			return step;
		}

		@Override
		public Integer visitOn(POnCondition e, Integer step) {
			return step;
		}

		@Override
		public Integer visitScan(PScan e, Integer step) {
			return step + step(e, 0) + 2;
		}

		@Override
		public Integer visitRepeat(PRepeat e, Integer step) {
			return step + step(e, 0) + 2;
		}

		@Override
		public Integer visitTrap(PTrap e, Integer step) {
			return step + 1;
		}

	}

}

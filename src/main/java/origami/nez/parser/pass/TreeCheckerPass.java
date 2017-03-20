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

import origami.main.OOption;
import origami.nez.ast.SourcePosition;

import origami.nez.peg.Expression;
import origami.nez.peg.Grammar;
import origami.nez.peg.Production;
import origami.nez.peg.Typestate;

public class TreeCheckerPass extends CommonPass {

	boolean EnableDetree;

	public Expression check(Production p, OOption options) {
		this.options = options;
		this.EnableDetree = true;
		return this.checkProductionExpression(p.getUniqueName(), p.getExpression());
	}

	@Override
	public Grammar perform(Grammar g, OOption options) {
		this.options = options;
		this.EnableDetree = true;
		for (Production p : g) {
			g.setExpression(p.getLocalName(), this.checkProductionExpression(p.getUniqueName(), p.getExpression()));
		}
		return g;
	}

	protected SourcePosition src(Expression e) {
		return e.getSourceLocation();
	}

	@Override
	protected Object ref(Expression e) {
		return e.getExternalReference();
	}

	/* Typestate */

	private Typestate req;

	private final Typestate typeState(Expression e) {
		return Typestate.compute(e);
	}

	public Expression checkProductionExpression(String uname, Expression e) {
		this.req = typeState(e);
		// System.out.println("@@check " + uname + " typestate=" + this.req + "
		// " + e);
		return e.visit(this, null);
	}

	private Expression check(Expression e, int index, Typestate before, Typestate after) {
		this.req = before;
		e.set(index, e.get(index).visit(this, null));
		if (after != null) {
			this.req = after;
		}
		return e;
	}

	private Expression detree(Expression e, int index, Typestate req, Typestate after) {
		Expression ue = index == -1 ? e : e.get(index);
		if (req != Typestate.Unit) {
			options.reportWarning(src(ue), "removed mutation in %s", ue);
			if (!(ue instanceof Expression.PNonTerminal)) {
				this.req = Typestate.Unit;
				ue = ue.visit(this, null);
			}
			ue = new Expression.PDetree(ue, ref(ue));
		}
		if (after != null) {
			this.req = after;
		}
		if (index == -1) {
			return ue;
		}
		e.set(index, ue);
		return e;
	}

	private Expression insertLink(Expression e, int index) {
		if (index == -1) {
			options.reportNotice(src(e), "inserted unlabeled link");
			return new Expression.PLinkTree(null, e, ref(e));
		} else {
			options.reportNotice(src(e.get(index)), "inserted unlabeled link");
			e.set(index, new Expression.PLinkTree(null, e.get(index), ref(e.get(index))));
			return e;
		}
	}

	@Override
	public Expression visitNonTerminal(Expression.PNonTerminal n, Void a) {
		Production p = n.getProduction();
		Typestate innerState = typeState(n);
		// System.out.println("@@@ Production " + n + " inner=" + innerState + "
		// req=" + this.req);
		if (innerState == Typestate.Tree) {
			if (req == Typestate.TreeMutation) {
				return insertLink(n, -1);
			}
			if (req == Typestate.Unit || req == Typestate.Immutation) {
				return detree(n, -1, innerState, this.req);
			}
			this.req = Typestate.Immutation;
			return n;
		}
		if (innerState == Typestate.TreeMutation) {
			if (req != Typestate.TreeMutation) {
				return detree(n, -1, innerState, req);
			}
			return n;
		}
		if (innerState == Typestate.Fold) {
			if (req == Typestate.Immutation) {
				return n;
			}
			return detree(n, -1, innerState, req);
		}
		return n;
	}

	@Override
	public Expression visitTree(Expression.PTree p, Void a) {
		Typestate innerState = typeState(p.get(0));
		if (p.folding) {
			if (req != Typestate.Immutation) {
				options.reportWarning(src(p), "removed tree folding %s", p);
				detree(p, 0, innerState, this.req);
				return p.get(0);
			}
			return check(p, 0, Typestate.TreeMutation, Typestate.Immutation);
		} else {
			if (req == Typestate.TreeMutation) {
				check(p, 0, Typestate.TreeMutation, Typestate.TreeMutation);
				insertLink(p, 0);
				return p.get(0);
			}
			if (req != Typestate.Tree) {
				options.reportWarning(src(p), "removed tree %s (req=%s)", p, req);
				detree(p, 0, innerState, this.req);
				return p.get(0);
			}
			return check(p, 0, Typestate.TreeMutation, Typestate.Immutation);
		}
	}

	@Override
	public Expression visitTag(Expression.PTag p, Void a) {
		if (this.req != Typestate.TreeMutation) {
			options.reportWarning(src(p), "removed %s", p);
			return Expression.defaultEmpty;
		}
		return super.visitTag(p, a);
	}

	@Override
	public Expression visitReplace(Expression.PReplace p, Void a) {
		if (this.req != Typestate.TreeMutation) {
			options.reportWarning(src(p), "removed %s", p);
			return Expression.defaultEmpty;
		}
		return p;
	}

	@Override
	public Expression visitLinkTree(Expression.PLinkTree p, Void a) {
		Typestate innerState = typeState(p.get(0));
		if (this.req == Typestate.TreeMutation) {
			this.check(p, 0, innerState, Typestate.TreeMutation);
			if (innerState != Typestate.Tree) {
				p.set(0, new Expression.PTree(p.get(0), ref(p)));
			}
			return p;
		} else {
			detree(p, 0, innerState, this.req);
			return p.get(0);
		}
	}

	@Override
	public Expression visitChoice(Expression.PChoice p, Void a) {
		Typestate req = this.req;
		Typestate next = this.req;
		for (int i = 0; i < p.size(); i++) {
			check(p, i, req, null);
			if (this.req != req && this.req != next) {
				next = this.req;
			}
		}
		this.req = next;
		return p;
	}

	@Override
	public Expression visitDispatch(Expression.PDispatch p, Void a) {
		Typestate req = this.req;
		Typestate next = this.req;
		for (int i = 0; i < p.size(); i++) {
			check(p, i, req, null);
			if (this.req != req && this.req != next) {
				next = this.req;
			}
		}
		this.req = next;
		return p;
	}

	@Override
	public Expression visitRepetition(Expression.PRepetition p, Void a) {
		return this.visitUnary(p, a);
	}

	@Override
	public Expression visitOption(Expression.POption p, Void a) {
		return this.visitUnary(p, a);
	}

	@Override
	public Expression visitAnd(Expression.PAnd p, Void a) {
		return this.visitUnary(p, a);
	}

	private Expression visitUnary(Expression.PUnary p, Void a) {
		Typestate innerState = typeState(p.get(0));
		if (innerState == Typestate.Tree) {
			if (this.req == Typestate.TreeMutation) {
				check(p, 0, Typestate.Tree, this.req);
				return insertLink(p, 0);
			} else {
				return detree(p, 0, innerState, this.req);
			}
		}
		p.set(0, p.get(0).visit(this, a));
		return p;
	}

	@Override
	public Expression visitNot(Expression.PNot p, Void a) {
		Typestate innerState = typeState(p.get(0));
		return detree(p, 0, innerState, this.req);
	}
}

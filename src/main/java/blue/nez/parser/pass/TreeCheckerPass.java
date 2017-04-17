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

import blue.nez.ast.SourcePosition;
import blue.nez.peg.Expression;
import blue.nez.peg.Grammar;
import blue.nez.peg.Production;
import blue.nez.peg.Typestate;
import blue.nez.peg.expression.PAnd;
import blue.nez.peg.expression.PChoice;
import blue.nez.peg.expression.PDetree;
import blue.nez.peg.expression.PDispatch;
import blue.nez.peg.expression.PLinkTree;
import blue.nez.peg.expression.PNonTerminal;
import blue.nez.peg.expression.PNot;
import blue.nez.peg.expression.POption;
import blue.nez.peg.expression.PRepetition;
import blue.nez.peg.expression.PReplace;
import blue.nez.peg.expression.PTag;
import blue.nez.peg.expression.PTree;
import blue.nez.peg.expression.PUnary;
import blue.origami.util.OOption;

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
		return e.getSourcePosition();
	}

	/* Typestate */

	private Typestate req;

	private final Typestate typeState(Expression e) {
		return Typestate.compute(e);
	}

	public Expression checkProductionExpression(String uname, Expression e) {
		this.req = this.typeState(e);
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
			this.options.reportWarning(this.src(ue), "removed mutation in %s", ue);
			if (!(ue instanceof PNonTerminal)) {
				this.req = Typestate.Unit;
				ue = ue.visit(this, null);
			}
			ue = new PDetree(ue);
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
			this.options.reportNotice(this.src(e), "inserted unlabeled link");
			return new PLinkTree(null, e);
		} else {
			this.options.reportNotice(this.src(e.get(index)), "inserted unlabeled link");
			e.set(index, new PLinkTree(null, e.get(index)));
			return e;
		}
	}

	@Override
	public Expression visitNonTerminal(PNonTerminal n, Void a) {
		Production p = n.getProduction();
		Typestate innerState = this.typeState(n);
		// System.out.println("@@@ Production " + n + " inner=" + innerState + "
		// req=" + this.req);
		if (innerState == Typestate.Tree) {
			if (this.req == Typestate.TreeMutation) {
				return this.insertLink(n, -1);
			}
			if (this.req == Typestate.Unit || this.req == Typestate.Immutation) {
				return this.detree(n, -1, innerState, this.req);
			}
			this.req = Typestate.Immutation;
			return n;
		}
		if (innerState == Typestate.TreeMutation) {
			if (this.req != Typestate.TreeMutation) {
				return this.detree(n, -1, innerState, this.req);
			}
			return n;
		}
		if (innerState == Typestate.Fold) {
			if (this.req == Typestate.Immutation) {
				return n;
			}
			return this.detree(n, -1, innerState, this.req);
		}
		return n;
	}

	@Override
	public Expression visitTree(PTree p, Void a) {
		Typestate innerState = this.typeState(p.get(0));
		if (p.folding) {
			if (this.req != Typestate.Immutation) {
				this.options.reportWarning(this.src(p), "removed tree folding %s", p);
				this.detree(p, 0, innerState, this.req);
				return p.get(0);
			}
			return this.check(p, 0, Typestate.TreeMutation, Typestate.Immutation);
		} else {
			if (this.req == Typestate.TreeMutation) {
				this.check(p, 0, Typestate.TreeMutation, Typestate.TreeMutation);
				this.insertLink(p, 0);
				return p.get(0);
			}
			if (this.req != Typestate.Tree) {
				this.options.reportWarning(this.src(p), "removed tree %s (req=%s)", p, this.req);
				this.detree(p, 0, innerState, this.req);
				return p.get(0);
			}
			return this.check(p, 0, Typestate.TreeMutation, Typestate.Immutation);
		}
	}

	@Override
	public Expression visitTag(PTag p, Void a) {
		if (this.req != Typestate.TreeMutation) {
			this.options.reportWarning(this.src(p), "removed %s", p);
			return Expression.defaultEmpty;
		}
		return super.visitTag(p, a);
	}

	@Override
	public Expression visitReplace(PReplace p, Void a) {
		if (this.req != Typestate.TreeMutation) {
			this.options.reportWarning(this.src(p), "removed %s", p);
			return Expression.defaultEmpty;
		}
		return p;
	}

	@Override
	public Expression visitLinkTree(PLinkTree p, Void a) {
		Typestate innerState = this.typeState(p.get(0));
		if (this.req == Typestate.TreeMutation) {
			this.check(p, 0, innerState, Typestate.TreeMutation);
			if (innerState != Typestate.Tree) {
				p.set(0, new PTree(p.get(0)));
			}
			return p;
		} else {
			this.detree(p, 0, innerState, this.req);
			return p.get(0);
		}
	}

	@Override
	public Expression visitChoice(PChoice p, Void a) {
		Typestate req = this.req;
		Typestate next = this.req;
		for (int i = 0; i < p.size(); i++) {
			this.check(p, i, req, null);
			if (this.req != req && this.req != next) {
				next = this.req;
			}
		}
		this.req = next;
		return p;
	}

	@Override
	public Expression visitDispatch(PDispatch p, Void a) {
		Typestate req = this.req;
		Typestate next = this.req;
		for (int i = 0; i < p.size(); i++) {
			this.check(p, i, req, null);
			if (this.req != req && this.req != next) {
				next = this.req;
			}
		}
		this.req = next;
		return p;
	}

	@Override
	public Expression visitRepetition(PRepetition p, Void a) {
		return this.visitUnary(p, a);
	}

	@Override
	public Expression visitOption(POption p, Void a) {
		return this.visitUnary(p, a);
	}

	@Override
	public Expression visitAnd(PAnd p, Void a) {
		return this.visitUnary(p, a);
	}

	private Expression visitUnary(PUnary p, Void a) {
		Typestate innerState = this.typeState(p.get(0));
		if (innerState == Typestate.Tree) {
			if (this.req == Typestate.TreeMutation) {
				this.check(p, 0, Typestate.Tree, this.req);
				return this.insertLink(p, 0);
			} else {
				return this.detree(p, 0, innerState, this.req);
			}
		}
		p.set(0, p.get(0).visit(this, a));
		return p;
	}

	@Override
	public Expression visitNot(PNot p, Void a) {
		Typestate innerState = this.typeState(p.get(0));
		return this.detree(p, 0, innerState, this.req);
	}
}

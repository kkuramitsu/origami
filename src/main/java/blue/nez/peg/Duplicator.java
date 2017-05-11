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
import blue.nez.peg.expression.PScan;
import blue.nez.peg.expression.PSymbolAction;
import blue.nez.peg.expression.PSymbolPredicate;
import blue.nez.peg.expression.PSymbolScope;
import blue.nez.peg.expression.PTag;
import blue.nez.peg.expression.PTrap;
import blue.nez.peg.expression.PTree;
import blue.nez.peg.expression.PValue;

public class Duplicator<A> extends AbstractExpressionVisitor<A> {

	public Duplicator(Grammar grammar) {
		super(grammar);
	}

	protected boolean enableFullDuplication = false;

	protected Expression dup(Expression e, A a) {
		return e.visit(this, a);
	}

	protected Expression dup(Expression e, int i, A a) {
		return e.get(i).visit(this, a);
	}

	@Override
	public Expression visitNonTerminal(PNonTerminal e, A a) {
		if (e.getGrammar() != this.base && this.base != null) {
			String lname = e.getLocalName();
			return new PNonTerminal(this.base, e.getNameSpace(), lname);
		}
		if (this.enableFullDuplication) {
			return new PNonTerminal(e.getGrammar(), e.getLocalName());
		}
		return e;
	}

	@Override
	public Expression visitEmpty(PEmpty e, A a) {
		if (this.enableFullDuplication) {
			return new PEmpty();
		}
		return e;
	}

	@Override
	public Expression visitFail(PFail e, A a) {
		if (this.enableFullDuplication) {
			return new PFail();
		}
		return e;
	}

	@Override
	public Expression visitByte(PByte e, A a) {
		if (this.enableFullDuplication) {
			return new PByte(e.byteChar());
		}
		return e;
	}

	@Override
	public Expression visitByteSet(PByteSet e, A a) {
		if (this.enableFullDuplication) {
			return new PByteSet(e.byteSet());
		}
		return e;
	}

	@Override
	public Expression visitAny(PAny e, A a) {
		if (this.enableFullDuplication) {
			return new PAny();
		}
		return e;
	}

	@Override
	public Expression visitPair(PPair e, A a) {
		Expression e0 = this.dup(e, 0, a);
		Expression e1 = this.dup(e, 1, a);
		if (e0 != e.get(0) || e1 != e.get(1) || this.enableFullDuplication) {
			return Expression.newSequence(e0, e1);
		}
		return e;
	}

	@Override
	public Expression visitChoice(PChoice e, A a) {
		boolean isModified = false;
		Expression[] en = new Expression[e.size()];
		for (int i = 0; i < e.size(); i++) {
			en[i] = this.dup(e, i, a);
			if (en[i] != e.get(i)) {
				isModified = true;
			}
		}
		if (isModified || this.enableFullDuplication) {
			return new PChoice(e.isUnordered(), en);
		}
		return e;
	}

	@Override
	public Expression visitDispatch(PDispatch e, A a) {
		boolean isModified = false;
		Expression[] en = new Expression[e.size()];
		for (int i = 0; i < e.size(); i++) {
			en[i] = this.dup(e, i, a);
			if (en[i] != e.get(i)) {
				isModified = true;
			}
		}
		if (isModified || this.enableFullDuplication) {
			return new PDispatch(en, e.indexMap);
		}
		return e;
	}

	@Override
	public Expression visitOption(POption e, A a) {
		Expression e0 = this.dup(e, 0, a);
		if (e0 != e.get(0) || this.enableFullDuplication) {
			return new POption(e0);
		}
		return e;
	}

	@Override
	public Expression visitRepetition(PRepetition e, A a) {
		Expression e0 = this.dup(e, 0, a);
		if (e0 != e.get(0) || this.enableFullDuplication) {
			return new PRepetition(e0, e.min, e.max);
		}
		return e;
	}

	@Override
	public Expression visitAnd(PAnd e, A a) {
		Expression e0 = this.dup(e, 0, a);
		if (e0 != e.get(0) || this.enableFullDuplication) {
			return new PAnd(e0);
		}
		return e;
	}

	@Override
	public Expression visitNot(PNot e, A a) {
		Expression e0 = this.dup(e, 0, a);
		if (e0 != e.get(0) || this.enableFullDuplication) {
			return new PNot(e0);
		}
		return e;
	}

	@Override
	public Expression visitTree(PTree e, A a) {
		Expression e0 = this.dup(e, 0, a);
		if (e0 != e.get(0) || this.enableFullDuplication) {
			return new PTree(e.folding, e.label, e.beginShift, e0, e.tag, e.value, e.endShift);
		}
		return e;
	}

	@Override
	public Expression visitLinkTree(PLinkTree e, A a) {
		Expression e0 = this.dup(e, 0, a);
		if (e0 != e.get(0) || this.enableFullDuplication) {
			return new PLinkTree(e.label, e0);
		}
		return e;
	}

	@Override
	public Expression visitTag(PTag e, A a) {
		if (this.enableFullDuplication) {
			return new PTag(e.tag);
		}
		return e;
	}

	@Override
	public Expression visitValue(PValue e, A a) {
		if (this.enableFullDuplication) {
			return new PValue(e.value);
		}
		return e;
	}

	@Override
	public Expression visitDetree(PDetree e, A a) {
		Expression e0 = this.dup(e, 0, a);
		if (e0 != e.get(0) || this.enableFullDuplication) {
			return new PDetree(e0);
		}
		return e;
	}

	@Override
	public Expression visitSymbolScope(PSymbolScope e, A a) {
		Expression e0 = this.dup(e, 0, a);
		if (e0 != e.get(0) || this.enableFullDuplication) {
			return new PSymbolScope(e.label, e0);
		}
		return e;
	}

	@Override
	public Expression visitSymbolAction(PSymbolAction e, A a) {
		Expression e0 = this.dup(e, 0, a);
		if (e0 != e.get(0) || this.enableFullDuplication) {
			return new PSymbolAction(e.action, e.label, (PNonTerminal) e0);
		}
		return e;
	}

	@Override
	public Expression visitSymbolPredicate(PSymbolPredicate e, A a) {
		Expression e0 = this.dup(e, 0, a);
		if (e0 != e.get(0) || this.enableFullDuplication) {
			return new PSymbolPredicate(e.pred, e.label, (PNonTerminal) e0, e.thunk);
		}
		return e;
	}

	@Override
	public Expression visitScan(PScan e, A a) {
		Expression e0 = this.dup(e, 0, a);
		if (e0 != e.get(0) || this.enableFullDuplication) {
			return new PScan(e.pattern, e0);
		}
		return e;
	}

	@Override
	public Expression visitRepeat(PRepeat e, A a) {
		Expression e0 = this.dup(e, 0, a);
		if (e0 != e.get(0) || this.enableFullDuplication) {
			return new PRepeat(e0);
		}
		return e;
	}

	@Override
	public Expression visitIf(PIfCondition e, A a) {
		if (this.enableFullDuplication) {
			return new PIfCondition(e.nflag);
		}
		return e;
	}

	@Override
	public Expression visitOn(POnCondition e, A a) {
		Expression e0 = this.dup(e, 0, a);
		if (e0 != e.get(0) || this.enableFullDuplication) {
			return new POnCondition(e.nflag, e0);
		}
		return e;
	}

	@Override
	public Expression visitTrap(PTrap e, A a) {
		if (this.enableFullDuplication) {
			return new PTrap(e.trapid, e.uid);
		}
		return e;
	}

}
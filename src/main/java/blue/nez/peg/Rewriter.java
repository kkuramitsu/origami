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

public class Rewriter<A> extends ExpressionVisitor<Expression, A> {

	protected Expression rewrite(Expression e, A a) {
		return e.visit(this, a);
	}

	protected Expression rewrite(Expression e, int index, A a) {
		return e.get(index).visit(this, a);
	}

	@Override
	public Expression visitNonTerminal(PNonTerminal e, A a) {
		return e;
	}

	@Override
	public Expression visitEmpty(PEmpty e, A a) {
		return e;
	}

	@Override
	public Expression visitFail(PFail e, A a) {
		return e;
	}

	@Override
	public Expression visitByte(PByte e, A a) {
		return e;
	}

	@Override
	public Expression visitByteSet(PByteSet e, A a) {
		return e;
	}

	@Override
	public Expression visitAny(PAny e, A a) {
		return e;
	}

	@Override
	public Expression visitPair(PPair e, A a) {
		e.set(0, this.rewrite(e, 0, a));
		e.set(1, this.rewrite(e, 1, a));
		return e.desugar();
	}

	@Override
	public Expression visitChoice(PChoice e, A a) {
		for (int i = 0; i < e.size(); i++) {
			e.set(i, this.rewrite(e, i, a));
		}
		return e;
	}

	@Override
	public Expression visitOption(POption e, A a) {
		e.set(0, this.rewrite(e, 0, a));
		return e;
	}

	@Override
	public Expression visitRepetition(PRepetition e, A a) {
		e.set(0, this.rewrite(e, 0, a));
		return e;
	}

	@Override
	public Expression visitAnd(PAnd e, A a) {
		e.set(0, this.rewrite(e, 0, a));
		return e;
	}

	@Override
	public Expression visitNot(PNot e, A a) {
		e.set(0, this.rewrite(e, 0, a));
		return e;
	}

	@Override
	public Expression visitDetree(PDetree e, A a) {
		e.set(0, this.rewrite(e, 0, a));
		return e;
	}

	@Override
	public Expression visitTree(PTree e, A a) {
		e.set(0, this.rewrite(e, 0, a));
		return e;
	}

	@Override
	public Expression visitLinkTree(PLinkTree e, A a) {
		e.set(0, this.rewrite(e, 0, a));
		return e;
	}

	@Override
	public Expression visitTag(PTag e, A a) {
		return e;
	}

	@Override
	public Expression visitValue(PValue e, A a) {
		return e;
	}

	@Override
	public Expression visitSymbolScope(PSymbolScope e, A a) {
		e.set(0, this.rewrite(e, 0, a));
		return e;
	}

	@Override
	public Expression visitSymbolAction(PSymbolAction e, A a) {
		e.set(0, this.rewrite(e, 0, a));
		return e;
	}

	@Override
	public Expression visitSymbolPredicate(PSymbolPredicate e, A a) {
		e.set(0, this.rewrite(e, 0, a));
		return e;
	}

	@Override
	public Expression visitScan(PScan e, A a) {
		e.set(0, this.rewrite(e, 0, a));
		return e;
	}

	@Override
	public Expression visitRepeat(PRepeat e, A a) {
		e.set(0, this.rewrite(e, 0, a));
		return e;
	}

	@Override
	public Expression visitIf(PIfCondition e, A a) {
		return e;
	}

	@Override
	public Expression visitOn(POnCondition e, A a) {
		e.set(0, this.rewrite(e, 0, a));
		return e;
	}

	@Override
	public Expression visitTrap(PTrap e, A a) {
		return e;
	}

	@Override
	public Expression visitDispatch(PDispatch e, A a) {
		for (int i = 1; i < e.inners.length; i++) {
			e.inners[i] = this.rewrite(e, i, a);
		}
		return e;
	}
}
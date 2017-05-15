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
	public Expression visitMany(PMany e, A a) {
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
	public Expression visitIf(PIf e, A a) {
		return e;
	}

	@Override
	public Expression visitOn(POn e, A a) {
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
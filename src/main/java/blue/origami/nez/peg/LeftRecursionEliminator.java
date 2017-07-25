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
import blue.origami.util.OOption;
import blue.origami.util.OptionalFactory;

public class LeftRecursionEliminator extends ExpressionVisitor<Void, Void>
		implements OptionalFactory<LeftRecursionEliminator> {

	public void compute(Grammar g) {
		Expression root = g.getStartProduction().getExpression();
		root.visit(this, null);
	}

	@Override
	public LeftRecursionEliminator clone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(OOption options) {
		// TODO Auto-generated method stub

	}

	@Override
	public Void visitTrap(PTrap e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitTrap : " + e.toString());
		return null;
	}

	@Override
	public Void visitNonTerminal(PNonTerminal e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitNonTerminal : " + e.getUniqueName());
		return null;
	}

	@Override
	public Void visitEmpty(PEmpty e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitEmpty : " + e.toString());
		return null;
	}

	@Override
	public Void visitFail(PFail e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitFail : " + e.toString());
		return null;
	}

	@Override
	public Void visitByte(PByte e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitByte : " + e.toString());
		return null;
	}

	@Override
	public Void visitByteSet(PByteSet e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitByteSet : " + e.toString());
		return null;
	}

	@Override
	public Void visitAny(PAny e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitAny : " + e.toString());
		return null;
	}

	@Override
	public Void visitPair(PPair e, Void a) {
		System.out.println("visitPair");

		e.left.visit(this, a);
		e.right.visit(this, a);
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitChoice(PChoice e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitChoice : " + e.toString());
		return null;
	}

	@Override
	public Void visitDispatch(PDispatch e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitDispatch : " + e.toString());
		return null;
	}

	@Override
	public Void visitOption(POption e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitOption : " + e.toString());
		return null;
	}

	@Override
	public Void visitMany(PMany e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitMany : " + e.toString());
		return null;
	}

	@Override
	public Void visitAnd(PAnd e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitAnd : " + e.toString());
		return null;
	}

	@Override
	public Void visitNot(PNot e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitNot : " + e.toString());
		return null;
	}

	@Override
	public Void visitTree(PTree e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitTree : " + e.toString());
		return null;
	}

	@Override
	public Void visitDetree(PDetree e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitDetree : " + e.toString());
		return null;
	}

	@Override
	public Void visitLinkTree(PLinkTree e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitLinkTree : " + e.toString());
		return null;
	}

	@Override
	public Void visitTag(PTag e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitTag : " + e.toString());
		return null;
	}

	@Override
	public Void visitValue(PValue e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitValue : " + e.toString());
		return null;
	}

	@Override
	public Void visitSymbolScope(PSymbolScope e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitSymbolScope : " + e.toString());
		return null;
	}

	@Override
	public Void visitSymbolAction(PSymbolAction e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitSymbolAction : " + e.toString());
		return null;
	}

	@Override
	public Void visitSymbolPredicate(PSymbolPredicate e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitSymbolPredicate : " + e.toString());
		return null;
	}

	@Override
	public Void visitIf(PIf e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitIf : " + e.toString());
		return null;
	}

	@Override
	public Void visitOn(POn e, Void a) {
		// TODO Auto-generated method stub
		System.out.println("visitOn : " + e.toString());
		return null;
	}

	@Override
	public Class<?> keyClass() {
		// TODO Auto-generated method stub
		return null;
	}
}

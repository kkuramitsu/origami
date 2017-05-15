package blue.origami.nez.peg.expression;

import blue.origami.nez.peg.Expression;
import blue.origami.nez.peg.ExpressionVisitor;

/**
 * Expression.Not represents a not-predicate !e.
 * 
 * @author kiki
 *
 */

public class PNot extends PUnary {

	public PNot(Expression e) {
		super(e);
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitNot(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.formatUnary("!", null, sb);
	}

}
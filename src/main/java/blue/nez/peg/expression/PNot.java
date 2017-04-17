package blue.nez.peg.expression;

import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;

/**
 * Expression.Not represents a not-predicate !e.
 * 
 * @author kiki
 *
 */

public class PNot extends PUnary {
	public PNot(Expression e, Object ref) {
		super(e, ref);
	}

	public PNot(Expression e) {
		super(e, null);
	}

	@Override
	public final boolean equals(Object o) {
		if (o instanceof PNot) {
			return this.get(0).equals(((Expression) o).get(0));
		}
		return false;
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
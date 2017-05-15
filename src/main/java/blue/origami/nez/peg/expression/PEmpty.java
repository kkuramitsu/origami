package blue.origami.nez.peg.expression;

import blue.origami.nez.peg.ExpressionVisitor;

/**
 * The Expression.Empty represents an empty expression, denoted '' in
 * Expression.
 * 
 * @author kiki
 *
 */

public class PEmpty extends PTerm {
	public PEmpty() {
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitEmpty(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("''");
	}
}
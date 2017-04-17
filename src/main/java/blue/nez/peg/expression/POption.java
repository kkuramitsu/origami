package blue.nez.peg.expression;

import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;

/**
 * Expression.Option represents an optional expression e?
 * 
 * @author kiki
 *
 */

public class POption extends PUnary {
	public POption(Expression e, Object ref) {
		super(e, ref);
	}

	public POption(Expression e) {
		super(e, null);
	}

	@Override
	public final boolean equals(Object o) {
		if (o instanceof POption) {
			return this.get(0).equals(((Expression) o).get(0));
		}
		return false;
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitOption(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.formatUnary(null, "?", sb);
	}

}
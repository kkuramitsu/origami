package blue.nez.peg.expression;

import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;

/**
 * Expression.Repetition is used to identify a common property of ZeroMore
 * and OneMore expressions.
 * 
 * @author kiki
 *
 */

public class PRepetition extends PUnary {
	public final int min;
	public final int max;

	public final boolean isOneMore() {
		return this.min > 0;
	}

	public PRepetition(Expression e, int min, int max, Object ref) {
		super(e, ref);
		this.min = min;
		this.max = max;
	}

	public PRepetition(Expression e, int min, Object ref) {
		this(e, min, -1, ref);
	}

	public PRepetition(Expression e, int min, int max) {
		this(e, min, max, null);
	}

	public PRepetition(Expression e, int min) {
		this(e, min, -1, null);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof PRepetition) {
			PRepetition r = (PRepetition) o;
			return (this.min == r.min && this.max == r.max && this.get(0).equals(r.get(0)));
		}
		return false;
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitRepetition(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.formatUnary(null, this.isOneMore() ? "+" : "*", sb);
	}

}
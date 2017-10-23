package blue.origami.parser.peg;

/**
 * Expression.Repetition is used to identify a common property of ZeroMore and
 * OneMore expressions.
 * 
 * @author kiki
 *
 */

public class PMany extends PUnary {
	public final int min;
	public final int max;

	public PMany(Expression e, int min, int max) {
		super(e);
		this.min = min;
		this.max = max;
	}

	public PMany(Expression e, int min) {
		this(e, min, -1);
	}

	public final boolean isOneMore() {
		return this.min > 0;
	}

	@Override
	protected Object[] extract() {
		return new Object[] { this.min, };
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitMany(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.formatUnary(null, this.isOneMore() ? "+" : "*", sb);
	}

}
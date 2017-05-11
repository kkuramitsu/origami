package blue.nez.peg;

public abstract class AbstractExpressionVisitor<A> extends ExpressionVisitor<Expression, A> {
	protected final Grammar base;

	public AbstractExpressionVisitor(Grammar base) {
		this.base = base;
	}

	protected boolean enableExternalDuplication = true; // FIXME: false

	protected Object ref(Expression e) {
		if (this.enableExternalDuplication) {
			return e.getRef();
		}
		return null;
	}

}
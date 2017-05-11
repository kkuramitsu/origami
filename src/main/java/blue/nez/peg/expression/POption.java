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

	public POption(Expression e) {
		super(e);
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
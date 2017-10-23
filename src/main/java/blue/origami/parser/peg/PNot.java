package blue.origami.parser.peg;

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
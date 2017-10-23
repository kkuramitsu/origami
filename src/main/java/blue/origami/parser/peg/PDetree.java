package blue.origami.parser.peg;

public class PDetree extends PUnary {
	public PDetree(Expression e) {
		super(e);
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitDetree(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.formatUnary("~", null, sb);
	}

}
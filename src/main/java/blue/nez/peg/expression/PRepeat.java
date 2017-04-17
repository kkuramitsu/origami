package blue.nez.peg.expression;

import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;

public class PRepeat extends PUnary {
	public PRepeat(Expression e) {
		super(e);
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitRepeat(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("<repeat ");
		this.get(0).strOut(sb);
		sb.append(">");
	}

}
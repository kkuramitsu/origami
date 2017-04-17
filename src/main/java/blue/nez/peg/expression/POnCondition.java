package blue.nez.peg.expression;

import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;

public class POnCondition extends PUnary {

	public final String nflag;

	public POnCondition(String flag, Expression e) {
		super(e);
		this.nflag = flag;
	}

	public final boolean isPositive() {
		return !this.nflag.startsWith("!");
	}

	public final String flagName() {
		return this.nflag.startsWith("!") ? this.nflag.substring(1) : this.nflag;
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("<if ");
		sb.append(this.nflag);
		sb.append(" ");
		this.get(0).strOut(sb);
		sb.append(">");
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitOn(this, a);
	}
}
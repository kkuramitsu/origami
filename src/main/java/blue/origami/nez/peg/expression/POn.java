package blue.origami.nez.peg.expression;

import blue.origami.nez.peg.Expression;
import blue.origami.nez.peg.ExpressionVisitor;

public class POn extends PUnary {

	public final String nflag;

	public POn(String flag, Expression e) {
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
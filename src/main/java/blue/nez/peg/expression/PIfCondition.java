package blue.nez.peg.expression;

import blue.nez.peg.ExpressionVisitor;

public class PIfCondition extends PTerm {
	public final String nflag;

	public PIfCondition(String flag) {
		this.nflag = flag;
	}

	public final boolean isPositive() {
		return !this.nflag.startsWith("!");
	}

	public final String flagName() {
		return this.nflag.startsWith("!") ? this.nflag.substring(1) : this.nflag;
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitIf(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("<if ");
		sb.append(this.nflag);
		sb.append(">");
	}

}
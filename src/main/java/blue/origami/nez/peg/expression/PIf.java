package blue.origami.nez.peg.expression;

import blue.origami.nez.peg.ExpressionVisitor;

public class PIf extends PTerm {
	public final String nflag;

	public PIf(String flag) {
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
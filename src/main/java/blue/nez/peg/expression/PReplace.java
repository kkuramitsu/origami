package blue.nez.peg.expression;

import blue.nez.peg.ExpressionVisitor;

public class PReplace extends PTerm {
	public String value;

	public PReplace(String value, Object ref) {
		super(ref);
		this.value = value;
	}

	@Override
	public final boolean equals(Object o) {
		if (o instanceof PReplace) {
			return this.value.equals(((PReplace) o).value);
		}
		return false;
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitReplace(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		formatReplace(this.value, sb);
	}
}
package blue.nez.peg.expression;

import blue.nez.peg.ExpressionVisitor;

public class PValue extends PTerm {
	public String value;

	public PValue(String value) {
		this.value = value;
	}

	@Override
	public final boolean equals(Object o) {
		if (o instanceof PValue) {
			return this.value.equals(((PValue) o).value);
		}
		return false;
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitValue(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		formatReplace(this.value, sb);
	}
}
package blue.origami.nez.peg.expression;

import blue.origami.nez.peg.ExpressionVisitor;

public class PValue extends PTerm {
	public String value;

	public PValue(String value) {
		this.value = value;
	}

	@Override
	protected Object[] extract() {
		return new Object[] { this.value };
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitValue(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append('`');
		for (int i = 0; i < this.value.length(); i++) {
			int c = this.value.charAt(i) & 0xff;
			ByteSet.formatByte(c, "`", "\\x02x", sb);
		}
		sb.append('`');
	}
}
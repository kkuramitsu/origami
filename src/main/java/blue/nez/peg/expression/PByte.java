package blue.nez.peg.expression;

import blue.nez.peg.ExpressionVisitor;

/**
 * Expression.Byte represents a single-byte string literal, denoted as 'a' in
 * Expression.
 *
 * @author kiki
 *
 */

public class PByte extends PTerm {
	/**
	 * byteChar
	 */
	public final ByteSet byteSet;

	public PByte(int byteChar) {
		this.byteSet = ByteSet.CharSet(byteChar & 0xff);
	}

	public final int byteChar() {
		return this.byteSet.getUnsignedByte();
	}

	@Override
	public final boolean equals(Object o) {
		if (o instanceof PByte) {
			return this.byteChar() == ((PByte) o).byteChar();
		}
		return false;
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitByte(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("'");
		ByteSet.formatByte(this.byteChar(), "'", "0x%02x", sb);
		sb.append("'");
	}
}
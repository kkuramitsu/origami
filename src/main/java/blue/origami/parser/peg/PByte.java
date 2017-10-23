package blue.origami.parser.peg;

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
		this.byteSet = ByteSet.byteChar(byteChar & 0xff);
	}

	public final ByteSet byteSet() {
		return this.byteSet;
	}

	public final int byteChar() {
		return this.byteSet.getUnsignedByte();
	}

	@Override
	public Object[] extract() {
		return new Object[] { this.byteSet };
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitByte(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("'");
		ByteSet.formatByte(this.byteChar(), "'", "\\x%02x", sb);
		sb.append("'");
	}
}
package blue.origami.nez.peg.expression;

import blue.origami.nez.peg.ExpressionVisitor;

/**
 * Expression.ByteSet is a bitmap-based representation of the character class
 * [X-y]
 * 
 * @author kiki
 *
 */

public class PByteSet extends PTerm {
	private ByteSet byteSet;

	public PByteSet() {
		this.byteSet = new ByteSet();
	}

	public PByteSet(ByteSet bs) {
		this.byteSet = bs;
	}

	public ByteSet byteSet() {
		return this.byteSet;
	}

	@Override
	public Object[] extract() {
		return new Object[] { this.byteSet };
	}

	public final boolean is(int n) {
		return this.byteSet.is(n);
	}

	public final void set(int n, boolean b) {
		this.byteSet.set(n, b);
	}

	public final void set(int s, int e, boolean b) {
		this.byteSet.set(s, e, b);
	}

	public final void union(PByteSet b) {
		this.byteSet = this.byteSet.union(b.byteSet);
	}

	// public final int n(int n) {
	// return this.bits[n];
	// }

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitByteSet(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("[");
		int start = -1;
		for (int i = 0; i < 256; i++) {
			if (start == -1 && this.is(i)) {
				start = i;
				continue;
			}
			if (start != -1 && !this.is(i)) {
				this.format(start, i - 1, sb);
				start = -1;
				continue;
			}
		}
		if (start != -1) {
			this.format(start, 255, sb);
		}
		sb.append("]");
	}

	private void format(int start, int end, StringBuilder sb) {
		ByteSet.formatByte(start, "-]", "\\x%02x", sb);
		if (start != end) {
			sb.append("-");
			ByteSet.formatByte(end, "-]", "\\x%02x", sb);
		}
	}
}
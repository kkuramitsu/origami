package blue.nez.peg.expression;

import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;

/**
 * Expression.ByteSet is a bitmap-based representation of the character class
 * [X-y]
 * 
 * @author kiki
 *
 */

public class PByteSet extends PTerm {
	// private ByteSet byteSet;
	public int bits[];

	public final boolean is(int n) {
		return (this.bits[n / 32] & (1 << (n % 32))) != 0;
	}

	public final void set(int n, boolean b) {
		if (b) {
			int mask = 1 << (n % 32);
			this.bits[n / 32] |= mask;
		} else {
			int mask = ~(1 << (n % 32));
			this.bits[n / 32] &= mask;
		}
	}

	public final void set(int s, int e, boolean b) {
		for (int i = s; i <= e; i++) {
			this.set(i, b);
		}
	}

	public final void union(PByteSet b) {
		for (int i = 0; i < this.bits.length; i++) {
			this.bits[i] = this.bits[i] | b.bits[i];
		}
	}

	public final boolean[] byteSet() {
		boolean[] b = new boolean[256];
		for (int i = 0; i < 256; i++) {
			b[i] = this.is(i);
		}
		return b;
	}

	public final int n(int n) {
		return this.bits[n];
	}

	public PByteSet(Object ref) {
		super(ref);
		this.bits = new int[8];
	}

	public PByteSet(int[] bits, Object ref) {
		this(ref);
		for (int i = 0; i < bits.length; i++) {
			this.bits[i] = bits[i];
		}
	}

	// public ByteSet(boolean[] b) {
	// this(b, null);
	// }

	@Override
	public final boolean equals(Object o) {
		if (o instanceof PByteSet) {
			PByteSet e = (PByteSet) o;
			for (int i = 0; i < this.bits.length; i++) {
				if (this.bits[i] != e.bits[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

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
		Expression.formatByte(start, "-]", "\\x%02x", sb);
		if (start != end) {
			sb.append("-");
			Expression.formatByte(end, "-]", "\\x%02x", sb);
		}
	}
}
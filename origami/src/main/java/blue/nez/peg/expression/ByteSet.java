package blue.nez.peg.expression;

import blue.origami.util.StringCombinator;

public class ByteSet implements StringCombinator {
	private final int bits[];

	public ByteSet() {
		this.bits = new int[8];
	}

	ByteSet(int[] bits) {
		this.bits = bits;
	}

	public final boolean is(int n) {
		return (this.bits[n / 32] & (1 << (n % 32))) != 0;
	}

	public void set(int n, boolean b) {
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

	// public final void union(ByteSet b) {
	// for (int i = 0; i < this.bits.length; i++) {
	// this.bits[i] = this.bits[i] | b.bits[i];
	// }
	// }

	public ByteSet union(ByteSet b) {
		ByteSet bs = new ByteSet();
		for (int i = 0; i < 256; i++) {
			bs.set(i, this.is(i) || b.is(i));
		}
		return bs;
	}

	public final boolean[] byteSet() {
		boolean[] b = new boolean[256];
		for (int i = 0; i < 256; i++) {
			b[i] = this.is(i);
		}
		return b;
	}

	public final int[] bits() {
		return this.bits;
	}

	@Override
	public final boolean equals(Object o) {
		if (o instanceof ByteSet) {
			ByteSet e = (ByteSet) o;
			for (int i = 0; i < this.bits.length; i++) {
				if (this.bits[i] != e.bits[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public int getUnsignedByte() {
		int single = -1;
		for (int i = 0; i < 256; i++) {
			if (this.is(i)) {
				if (single != -1) {
					return -1;
				}
				single = i;
			}
		}
		return single;
	}

	@Override
	public void strOut(StringBuilder sb) {
		int unsignedByte = this.getUnsignedByte();
		if (unsignedByte != -1) {
			sb.append("'");
			ByteSet.formatByte(unsignedByte, "'", "0x%02x", sb);
			sb.append("'");
		} else {
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
	}

	void format(int start, int end, StringBuilder sb) {
		ByteSet.formatByte(start, "-]", "\\x%02x", sb);
		if (start != end) {
			sb.append("-");
			ByteSet.formatByte(end, "-]", "\\x%02x", sb);
		}
	}

	public final static void formatByte(int ubyte, String escaped, String fmt, StringBuilder sb) {
		if (escaped.indexOf(ubyte) != -1) {
			sb.append("\\");
			sb.append((char) ubyte);
		}
		switch (ubyte) {
		case '\n':
			sb.append("\\n");
			return;
		case '\t':
			sb.append("\\t");
			return;
		case '\r':
			sb.append("\\r");
			return;
		case '\\':
			sb.append("\\\\");
			return;
		}
		if (Character.isISOControl(ubyte) || ubyte > 127) {
			sb.append(String.format(fmt/* "0x%02x" */, ubyte));
			return;
		}
		sb.append((char) ubyte);
	}

	//
	public final static ByteSet AnyChar = new AnyCharSet();

	static class AnyCharSet extends ByteSet {
		@Override
		public void strOut(StringBuilder sb) {
			sb.append(".");
		}
	}

	private final static CharSet[] CharSet = new CharSet[256];

	public static ByteSet CharSet(int ubyte) {
		if (CharSet[ubyte] == null) {
			CharSet[ubyte] = new CharSet(ubyte);
		}
		return CharSet[ubyte];
	}

	static class CharSet extends ByteSet {
		final int byteChar;

		CharSet(int byteChar) {
			super();
			this.byteChar = byteChar & 0xff;
			super.set(this.byteChar, true);
		}

		@Override
		public void set(int n, boolean b) {
		}

		@Override
		public int getUnsignedByte() {
			return this.byteChar;
		}

	}

}
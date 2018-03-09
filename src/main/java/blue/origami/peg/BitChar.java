package blue.origami.peg;

import blue.origami.common.OStrings;

class BitChar implements OStrings {
	private final int bits[];

	BitChar() {
		this.bits = new int[8];
	}

	BitChar(int[] bits) {
		this.bits = bits;
	}

	BitChar(byte s, byte e) {
		this();
		this.set2(s & 0xff, e & 0xff, true);
	}

	public final boolean is(int n) {
		return (this.bits[n / 32] & (1 << (n % 32))) != 0;
	}

	private void set2(int n, boolean b) {
		if (b) {
			int mask = 1 << (n % 32);
			this.bits[n / 32] |= mask;
		} else {
			int mask = ~(1 << (n % 32));
			this.bits[n / 32] &= mask;
		}
	}

	private void set2(int s, int e, boolean b) {
		if (e < s) {
			this.set2(e, s, b);
		} else {
			for (int i = s; i <= e; i++) {
				this.set2(i, b);
			}
		}
	}

	public BitChar not() {
		BitChar bs = new BitChar();
		for (int i = 0; i < 256; i++) {
			bs.set2(i, !this.is(i));
		}
		return bs;
	}

	public BitChar minus(BitChar a) {
		BitChar bs = new BitChar();
		for (int i = 0; i < 256; i++) {
			boolean b = this.is(i);
			if (a.is(i)) {
				b = false;
			}
			bs.set2(i, b);
		}
		return bs;
	}

	public BitChar and(BitChar b) {
		if (b.isAny()) {
			return this;
		}
		if (this.isAny()) {
			return b;
		}
		BitChar bs = new BitChar();
		for (int i = 0; i < 256; i++) {
			bs.set2(i, this.is(i) && b.is(i));
		}
		return bs;
	}

	public BitChar union(BitChar b) {
		if (b == null) {
			return this;
		}
		BitChar bs = new BitChar();
		for (int i = 0; i < 256; i++) {
			bs.set2(i, this.is(i) || b.is(i));
		}
		return bs;
	}

	public BitChar textVersion() {
		if (this.is(0)) {
			BitChar bs = new BitChar();
			for (int i = 1; i < 256; i++) {
				bs.set2(i, this.is(i));
			}
			return bs;
		}
		return this;
	}

	public final int[] bits() {
		return this.bits;
	}

	public final boolean[] bools() {
		boolean[] b = new boolean[256];
		for (int i = 0; i < 256; i++) {
			b[i] = this.is(i);
		}
		return b;
	}

	@Override
	public final boolean equals(Object o) {
		if (o instanceof BitChar) {
			BitChar e = (BitChar) o;
			for (int i = 0; i < this.bits.length; i++) {
				if (this.bits[i] != e.bits[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public boolean isAny() {
		for (int i = 0; i < 256; i++) {
			if (!this.is(i)) {
				return false;
			}
		}
		return true;
	}

	public boolean isSingle() {
		int c = 0;
		for (int i = 0; i < 256; i++) {
			if (this.is(i)) {
				c++;
				if (c > 1) {
					return false;
				}
			}
		}
		return c == 1;
	}

	public byte single() {
		for (int i = 0; i < 256; i++) {
			if (this.is(i)) {
				return (byte) i;
			}
		}
		return (byte) 0;
	}

	public boolean isBinary() {
		return this.is(0) && !this.isAny();
	}

	@Override
	public String toString() {
		return OStrings.stringfy(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		if (this.isAny()) {
			sb.append(".");
		} else if (this.isSingle()) {
			sb.append("'");
			BitChar.formatByte(this.single() & 0xff, "'", "\\x%02x", sb);
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
		BitChar.formatByte(start, "-]", "\\x%02x", sb);
		if (start != end) {
			sb.append("-");
			BitChar.formatByte(end, "-]", "\\x%02x", sb);
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

	private final static CharSet[] CharSet = new CharSet[256];

	public final static BitChar byteChar(byte b) {
		int ubyte = b & 0xff;
		if (CharSet[ubyte] == null) {
			CharSet[ubyte] = new CharSet(b);
		}
		return CharSet[ubyte];
	}

	static class CharSet extends BitChar {
		final byte byteChar;

		CharSet(byte byteChar) {
			super();
			this.byteChar = byteChar;
			super.set2(this.byteChar & 0xff, true);
		}

		@Override
		public boolean isAny() {
			return false;
		}

		@Override
		public boolean isSingle() {
			return true;
		}

		@Override
		public byte single() {
			return this.byteChar;
		}
	}

	final static BitChar AnySet = new AnySet();

	static class AnySet extends BitChar {
		AnySet() {
			super((byte) 0, (byte) 255);
		}

		@Override
		public boolean isAny() {
			return true;
		}

		@Override
		public boolean isSingle() {
			return false;
		}

		@Override
		public byte single() {
			return (byte) 0;
		}
	}

}
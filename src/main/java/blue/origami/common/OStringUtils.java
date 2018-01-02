package blue.origami.common;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;

public abstract class OStringUtils {

	public final static String newString(byte[] utf8) {
		try {
			return new String(utf8, "UTF8");
		} catch (UnsupportedEncodingException e) {
			OConsole.exit(1, e);
		}
		return new String(utf8);
	}

	public final static byte[] utf8(String text) {
		if (text == null) {
			return new byte[0];
		}
		try {
			return text.getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			OConsole.exit(1, e);
		}
		return text.getBytes();
	}

	public static boolean isValidUTF8(byte[] input) {
		CharsetDecoder cs = Charset.forName("UTF-8").newDecoder();
		try {
			cs.decode(ByteBuffer.wrap(input));
			return true;
		} catch (CharacterCodingException e) {
			return false;
		}
	}

	public static void formatBytes(StringBuilder sb, byte[] chunks) {
		sb.append("0x");
		for (byte c : chunks) {
			sb.append(String.format("%02x", c & 0xff));
		}
	}

	/* format */

	public final static void formatByte(StringBuilder sb, boolean quoted, int byteChar, String fmt, String escaped) {
		switch (byteChar) {
		case '\n':
			sb.append(quoted ? "'\\n'" : "\\n");
			return;
		case '\t':
			sb.append(quoted ? "'\\t'" : "\\t");
			return;
		case '\r':
			sb.append(quoted ? "'\\r'" : "\\r");
			return;
		case '\\':
			sb.append(quoted ? "'\\\\'" : "\\\\");
			return;
		}
		if (Character.isISOControl(byteChar) || byteChar > 127) {
			sb.append(String.format(fmt/* "0x%02x" */, byteChar));
			return;
		}
		if (quoted) {
			sb.append("'");
		}
		if (escaped.indexOf(byteChar) != -1) {
			sb.append("\\");
		}
		sb.append((char) byteChar);
		if (quoted) {
			sb.append("'");
		}
	}

	public final static void formatByte(StringBuilder sb, int byteChar) {
		formatByte(sb, true, byteChar, "0x%02x", "");
	}

	public final static void formatByteSet(StringBuilder sb, boolean[] b) {
		sb.append("[");
		for (int s = 0; s < 256; s++) {
			if (b[s]) {
				int e = searchEndChar(b, s + 1);
				if (s == e) {
					formatByte(sb, false, s, "\\x%02x", "-]");
				} else {
					formatByte(sb, false, s, "\\x%02x", "-]");
					sb.append("-");
					formatByte(sb, false, e, "\\x%02x", "-]");
					s = e;
				}
			}
		}
		sb.append("]");
	}

	private static int searchEndChar(boolean[] b, int s) {
		for (; s < 256; s++) {
			if (!b[s]) {
				return s - 1;
			}
		}
		return 255;
	}

	private final static char[] HexChar = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
			'f' };

	public final static void formatHexicalByteSet(StringBuilder sb, boolean[] b) {
		int end = 0;
		for (int c = 0; c < b.length; c++) {
			if (b[c]) {
				end = c + 1;
			}
		}
		for (int offset = 0; offset < end; offset += 4) {
			if (offset + 3 < b.length) {
				formatHedicalByteSet(sb, b, offset);
			}
		}
	}

	private static void formatHedicalByteSet(StringBuilder sb, boolean[] b, int offset) {
		int n = 0;
		if (b[offset + 0]) {
			n |= (1 << 3);
		}
		if (b[offset + 1]) {
			n |= (1 << 2);
		}
		if (b[offset + 2]) {
			n |= (1 << 1);
		}
		if (b[offset + 3]) {
			n |= 1;
		}
		sb.append(HexChar[n]);
	}

	public final static void formatStringLiteral(StringBuilder sb, char openChar, String text, char closeChar) {
		char slashChar = '\\';
		sb.append(openChar);
		int i = 0;
		for (; i < text.length(); i = i + 1) {
			char ch = text.charAt(i);
			if (ch == '\n') {
				sb.append(slashChar);
				sb.append("n");
			} else if (ch == '\t') {
				sb.append(slashChar);
				sb.append("t");
			} else if (ch == closeChar) {
				sb.append(slashChar);
				sb.append(ch);
			} else if (ch == '\\') {
				sb.append(slashChar);
				sb.append(slashChar);
			} else {
				sb.append(ch);
			}
		}
		sb.append(closeChar);
	}

	public final static void formatUTF8(StringBuilder sb, byte[] utf8) {
		String s = newString(utf8);
		if (Arrays.equals(utf8, utf8(s))) {
			formatStringLiteral(sb, '"', s, '"');
		} else {
			sb.append("'");
			for (byte c : utf8) {
				formatByte(sb, false, c & 0xff, "0x%02x", "-]'");
			}
			sb.append("'");
		}
	}

	public final static String quoteString(char openChar, String text, char closeChar) {
		StringBuilder sb = new StringBuilder();
		OStringUtils.formatStringLiteral(sb, openChar, text, closeChar);
		return sb.toString();
	}

	public final static String unquoteString(String text) {
		if (text.indexOf("\\") == -1) {
			return text;
		}
		CharReader r = new CharReader(text);
		StringBuilder sb = new StringBuilder();
		while (r.hasChar()) {
			char ch = r.readChar();
			if (ch == '0') {
				break;
			}
			sb.append(ch);
		}
		return sb.toString();
	}

	private static class CharReader {
		String text;
		int pos;

		public CharReader(String text) {
			this.text = text;
			this.pos = 0;
		}

		final boolean hasChar() {
			return (this.pos < this.text.length());
		}

		public final char readChar() {
			if (this.pos < this.text.length()) {
				char ch = this.read(this.pos);
				if (ch == '\\') {
					char ch1 = this.read(this.pos + 1);
					if (ch1 == 'u' || ch1 == 'U') {
						ch = OStringUtils.parseHex4(this.read(this.pos + 2), this.read(this.pos + 3),
								this.read(this.pos + 4), this.read(this.pos + 5));
						this.pos = this.pos + 5;
					} else {
						ch = this.readEsc(ch1);
						this.pos = this.pos + 1;
					}
				}
				this.pos = this.pos + 1;
				return ch;
			}
			return '\0';
		}

		private char read(int pos) {
			if (pos < this.text.length()) {
				return this.text.charAt(pos);
			}
			return 0;
		}

		private char readEsc(char ch1) {
			switch (ch1) {
			case '0':
			  return '\0'; /* null */
			case 'a':
				return '\007'; /* bel */
			case 'b':
				return '\b'; /* bs */
			case 'e':
				return '\033'; /* esc */
			case 'f':
				return '\f'; /* ff */
			case 'n':
				return '\n'; /* nl */
			case 'r':
				return '\r'; /* cr */
			case 't':
				return '\t'; /* ht */
			case 'v':
				return '\013'; /* vt */
			}
			return ch1;
		}
	}

	public final static String stringfyByteSet(boolean[] b) {
		StringBuilder sb = new StringBuilder();
		formatByteSet(sb, b);
		return sb.toString();
	}

	// USED
	public final static int parseInt(String text, int defval) {
		if (text.length() > 0) {
			try {
				return Integer.parseInt(text);
			} catch (NumberFormatException e) {
				// e.printStackTrace();
			}
		}
		return defval;
	}

	// Used in Factory.newCharClass
	public final static int parseAscii(String t) {
		if (t.startsWith("\\x")) {
			int c = OStringUtils.parseHexicalNumber(t.charAt(2));
			c = (c * 16) + OStringUtils.parseHexicalNumber(t.charAt(3));
			return c;
		}
		if (t.startsWith("\\u")) {
			return -1;
		}
		if (t.startsWith("\\") && t.length() > 1) {
			int c = t.charAt(1);
			switch (c) {
			// case 'a': return '\007'; /* bel */
			// case 'b': return '\b'; /* bs */
			// case 'e': return '\033'; /* esc */
			case 'f':
				return '\f'; /* ff */
			case 'n':
				return '\n'; /* nl */
			case 'r':
				return '\r'; /* cr */
			case 't':
				return '\t'; /* ht */
			case 'v':
				return '\013'; /* vt */
			}
			return c;
		}
		return -1;
	}

	public final static int parseUnicode(String t) {
		if (t.startsWith("\\u")) {
			int c = OStringUtils.parseHexicalNumber(t.charAt(2));
			c = (c * 16) + OStringUtils.parseHexicalNumber(t.charAt(3));
			c = (c * 16) + OStringUtils.parseHexicalNumber(t.charAt(4));
			c = (c * 16) + OStringUtils.parseHexicalNumber(t.charAt(5));
			return c;
		}
		return t.charAt(0);
	}

	// USED
	public static int parseHexicalNumber(int c) {
		if ('0' <= c && c <= '9') {
			return c - '0';
		}
		if ('a' <= c && c <= 'f') {
			return c - 'a' + 10;
		}
		if ('A' <= c && c <= 'F') {
			return c - 'A' + 10;
		}
		return 0;
	}

	public static char parseHex4(char ch1, char ch2, char ch3, char ch4) {
		int c = OStringUtils.parseHexicalNumber(ch1);
		c = (c * 16) + OStringUtils.parseHexicalNumber(ch2);
		c = (c * 16) + OStringUtils.parseHexicalNumber(ch3);
		c = (c * 16) + OStringUtils.parseHexicalNumber(ch4);
		return (char) c;
	}

	public final static String stringfyByte(int ch) {
		StringBuilder sb = new StringBuilder();
		formatByte(sb, ch);
		return sb.toString();
	}

	public final static String stringfyBitmap(boolean[] b) {
		StringBuilder sb = new StringBuilder();
		formatHexicalByteSet(sb, b);
		return sb.toString();
	}

}

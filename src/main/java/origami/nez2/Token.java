package origami.nez2;

import java.io.UnsupportedEncodingException;

public final class Token implements OStrings {
	final String token;
	final String path;
	final byte[] inputs;
	final int pos;
	final int epos;
	final int len;
	int linenum = -1;
	int start = -1;
	int col = -1;
	int end = -1;

	Token(String token, String path, byte[] inputs, int pos, int epos) {
		this.token = token;
		this.path = path == null ? "(unknown)" : path;
		this.inputs = inputs;
		this.pos = pos;
		this.epos = epos;
		this.len = this.inputs.length == 0 ? 0
				: (this.inputs[this.inputs.length - 1] == 0 ? this.inputs.length - 1 : this.inputs.length);
	}

	void check() {
		int line = 1;
		int start = 0;
		int col = 0;
		int end = -1;
		int pos = this.pos < this.len ? this.pos : this.len;
		for (int cur = 0; cur < pos; cur++) {
			if (this.inputs[cur] == '\n') {
				if (cur + 1 < pos) {
					start = cur + 1;
				}
				line++;
				col = -1;
			}
			col++;
		}
		if (this.pos < this.len) {
			if (this.inputs[this.pos] == '\n') {
				end = this.pos;
			}
		}
		if (end == -1) {
			end = this.len;
			for (int cur = this.pos; cur < this.len; cur++) {
				if (this.inputs[cur] == '\n') {
					end = cur;
				}
			}
		}
		this.linenum = line;
		this.start = start;
		this.col = col;
		this.end = end;
	}

	public String getSymbol() {
		return this.token;
	}

	public String getPath() {
		return this.path;
	}

	public int linenum() {
		if (this.linenum == -1) {
			this.check();
		}
		return this.linenum;
	}

	public int column() {
		if (this.linenum == -1) {
			this.check();
		}
		return this.col;
	}

	public String line() {
		try {
			return new String(this.inputs, this.start, this.end - this.start, "UTF8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return new String(this.inputs, this.start, this.end - this.start);
	}

	public String mark(char mark) {
		StringBuilder sb = new StringBuilder();
		for (int i = this.start; i < this.pos; i++) {
			if (this.inputs[i] == '\t') {
				sb.append('\t');
			} else {
				sb.append(' ');
			}
		}
		if (this.token != null && this.token.length() > 0) {
			for (int i = 0; i < this.token.length(); i++) {
				sb.append(mark);
			}
		} else {
			sb.append(mark);
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return OStrings.stringfy(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("[");
		sb.append(shortName(this.path));
		sb.append(":");
		sb.append(this.linenum());
		sb.append("] ");
		sb.append(this.token);
	}

	public static String shortName(String path) {
		return path.substring(path.lastIndexOf('/') + 1);
	}

}

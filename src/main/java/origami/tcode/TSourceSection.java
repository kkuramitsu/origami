package origami.tcode;

import origami.tcode.TSyntaxMapper.TCodeSection;

public class TSourceSection implements TCodeSection, TSyntax {
	TSyntaxMapper syntax;
	StringBuilder sb = new StringBuilder();
	int indent = 0;

	protected TSourceSection(TSyntaxMapper syntax, int indent) {
		this.syntax = syntax;
		this.indent = indent;
		this.TAB = syntax.getOrDefault(pTAB, "   ");
	}

	@Override
	public void emit(TCode c) {
		String fmt = c.getName();
		if (this.syntax.isDefined(fmt)) {
			fmt = this.syntax.get(c.getName());
		} else {
			fmt = fmt + this.syntax.getOrDefault(pApply, "(*%s*,%s*)*");
		}
		if (fmt.endsWith("*")) {
			String[] fmts = fmt.split("\\*");
			int cnt = 0;
			this.pushf(fmts[0]);
			for (TCode sub : c.subs()) {
				if (cnt > 0) {
					this.pushf(fmts[2], sub);
				} else {
					this.pushf(fmts[1], sub);
				}
				cnt++;
			}
			this.pushf(fmts[3]);
			return;
		}
		try {
			if (c.size() == 0) {
				Object o = c.getValue();
				this.push(String.format(fmt, o));
			} else {
				this.pushf(fmt, c.subs());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void incIndent() {
		this.indent++;
	}

	void decIndent() {
		assert (this.indent > 0);
		this.indent--;
	}

	private String LF = System.lineSeparator();
	private String TAB = "   ";

	private String Indent() {
		switch (this.indent) {
		case 0:
			return "";
		case 1:
			return this.TAB;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < this.indent; i++) {
			sb.append(this.TAB);
		}
		return sb.toString();
	}

	void push(String t) {
		this.sb.append(t);
	}

	void pushLine(String line) {
		this.sb.append(line);
		this.sb.append(this.LF);
	}

	void pushIndent(String line) {
		this.sb.append(this.Indent());
		this.sb.append(line);
	}

	void pushIndentLine(String line) {
		this.sb.append(this.Indent());
		this.pushLine(line);
	}

	void pushf(String format, TCode... args) {
		int start = 0;
		int index = 0;
		for (int i = 0; i < format.length(); i++) {
			char c = format.charAt(i);
			if (c == '\t') {
				this.pushSubstring(format, start, i);
				this.pushIndent("");
				start = i + 1;
			} else if (c == '\f') {
				this.pushSubstring(format, start, i);
				this.incIndent();
				start = i + 1;
			} else if (c == '\b') {
				this.pushSubstring(format, start, i);
				this.decIndent();
				start = i + 1;
			} else if (c == '\n') {
				this.pushSubstring(format, start, i);
				this.pushLine("");
				start = i + 1;
			} else if (c == '%') {
				this.pushSubstring(format, start, i);
				c = i + 1 < format.length() ? format.charAt(i + 1) : 0;
				if (c == 's') {
					this.pushArgument(args[index]);
					index++;
					i++;
				} else if (c == '%') {
					this.push("%");
					i++;
				} else if ('1' <= c && c <= '9') { // %1$s
					int n = c - '1';
					if (!(n < args.length)) {
						System.err.printf("FIXME n=%s  %d,%s\n", format, n, args.length);
					}
					this.pushArgument(args[n]);
					i += 3;
				}
				start = i + 1;
			}
		}
		this.pushSubstring(format, start, format.length());
	}

	public void pushArgument(Object value) {
		if (value instanceof TCode) {
			this.emit((TCode) value);
		} else {
			this.push(value.toString());
		}
	}

	private void pushSubstring(String format, int start, int end) {
		if (start < end) {
			this.push(format.substring(start, end));
		}
	}

	// void pushEnc(String key, Object first, int s, int e, IntConsumer f) {
	// this.pushfmt(this.syntax.s(key), first);
	// String delim = key + "delim";
	// for (int i = s; i < e; i++) {
	// if (i > s) {
	// this.push(this.syntax.symbol(delim, "delim", ","));
	// }
	// f.accept(i);
	// }
	// this.push(this.syntax.s("end " + key));
	// }
	//
	// void pushEnc(String key, Object first, int size, IntConsumer f) {
	// this.pushEnc(key, first, 0, size, f);
	// }
	//
	// void pushfmt(String format, Object... args) {
	// this.push(String.format(format, args));
	// }

	@Override
	public String toString() {
		return this.sb.toString();
	}

}
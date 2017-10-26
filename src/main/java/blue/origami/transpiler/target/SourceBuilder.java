package blue.origami.transpiler.target;

import java.util.function.IntConsumer;

import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.type.Ty;

abstract class SourceBuilder implements CodeSection {
	protected final SyntaxMapper syntax;
	protected final SourceTypeMapper ts;;
	StringBuilder sb = new StringBuilder();
	int indent = 0;

	SourceBuilder(SyntaxMapper syntax, SourceTypeMapper ts) {
		this.syntax = syntax;
		this.ts = ts;
	}

	void incIndent() {
		this.indent++;
	}

	void decIndent() {
		assert (this.indent > 0);
		this.indent--;
	}

	String Indent(String tab, String stmt) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < this.indent; i++) {
			sb.append(tab);
		}
		sb.append(stmt);
		return sb.toString();
	}

	void push(String t) {
		this.sb.append(t);
	}

	void pushLine(String line) {
		this.sb.append(line + "\n");
	}

	void pushIndent(String line) {
		this.sb.append(this.Indent("  ", line));
	}

	void pushIndentLine(String line) {
		this.sb.append(this.Indent("  ", line) + "\n");
	}

	@Override
	public String toString() {
		return this.sb.toString();
	}

	void pushE(Object value) {
		if (value instanceof Code) {
			((Code) value).emitCode(this);
		} else if (value instanceof Ty) {
			this.push(((Ty) value).mapType(this.ts));
		} else if (value instanceof SourceEmitter) {
			((SourceEmitter) value).emit((SourceSection) this);
		} else {
			this.push(value.toString());
		}
	}

	void pushf(String format, Object... args) {
		int start = 0;
		int index = 0;
		for (int i = 0; i < format.length(); i++) {
			char c = format.charAt(i);
			if (c == '\t') {
				this.pushBW(format, start, i);
				this.pushIndent("");
				start = i + 1;
			} else if (c == '%') {
				this.pushBW(format, start, i);
				c = i + 1 < format.length() ? format.charAt(i + 1) : 0;
				if (c == 's') {
					this.pushE(args[index]);
					index++;
					i++;
				} else if (c == '%') {
					this.push("%");
					i++;
				} else if ('1' <= c && c <= '9') { // %1$s
					int n = c - '1';
					if (!(n < args.length)) {
						System.out.printf("n=%s  %d,%s\n", format, n, args.length);
					}
					this.pushE(args[n]);
					i += 3;
				}
				start = i + 1;
			}
		}
		this.pushBW(format, start, format.length());
	}

	private void pushBW(String format, int start, int end) {
		if (start < end) {
			this.push(format.substring(start, end));
		}
	}

	void pushEnc(String key, Object first, int s, int e, IntConsumer f) {
		this.pushfmt(this.syntax.s(key), first);
		String delim = key + "delim";
		for (int i = s; i < e; i++) {
			if (i > s) {
				this.push(this.syntax.symbol(delim, "delim", ","));
			}
			f.accept(i);
		}
		this.push(this.syntax.s("end " + key));
	}

	void pushEnc(String key, Object first, int size, IntConsumer f) {
		this.pushEnc(key, first, 0, size, f);
	}

	void pushfmt(String format, Object... args) {
		this.push(String.format(format, args));
	}
}
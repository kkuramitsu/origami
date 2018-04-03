package blue.origami.common;

import java.util.function.Consumer;

import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.type.Ty;
import origami.nez2.OStrings;
import origami.nez2.Token;

public class TLog implements OStrings {
	public final static int Error = 0;
	public final static int Warning = 1;
	public final static int Notice = 2;
	public final static int Info = 3;
	public final static int None = 4;
	// public final static int Syntax = 1 << 4;
	// public final static int Type = 1 << 5;

	public Token s;
	public final int level;
	public final OFormat format;
	public final Object[] args;
	public TLog next = null;

	public TLog(Token s, int level, OFormat format, Object... args) {
		this.s = s == null ? UnknownPosition : s;
		this.level = level;
		this.format = format;
		this.args = filter(args);
		this.next = null;
	}

	public TLog() {
		this(null, None, TFmt.Checked);
	}

	static Object[] filter(Object... args) {
		class ArrayFormatter {
			Object[] a;
			String delim;

			ArrayFormatter(Object[] a, String delim) {
				this.a = a;
				this.delim = delim;
			}

			@Override
			public String toString() {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < this.a.length; i++) {
					if (i > 0) {
						sb.append(this.delim);
					}
					Object o = this.a[i];
					sb.append(o);
				}
				return sb.toString();
			}
		}
		for (int i = 0; i < args.length; i++) {
			if (args[i] != null && args[i].getClass().isArray()) {
				args[i] = new ArrayFormatter((Object[]) args[i], ",");
			}
			if (args[i] != null && args[i] instanceof Ty) {
				args[i] = ((Ty) args[i]).show();
			}
		}
		return args;
	}

	public static TLog append(TLog prev, TLog next) {
		if (prev == null) {
			return next;
		}
		prev.append(next);
		return prev;
	}

	public void append(TLog log) {
		TLog cur = this;
		for (; cur.next != null; cur = cur.next) {
			;
		}
		cur.next = log;
	}

	public TLog find(OFormat fmt) {
		String m2 = fmt.toString();
		for (TLog cur = this; cur != null; cur = cur.next) {
			if (cur.format == fmt) {
				return cur;
			}
			String m = cur.format.toString();

			if (m.startsWith(m2)) {
				return cur;
			}
		}
		return null;
	}

	public void setSource(Token s) {
		if (this.s == UnknownPosition && s != null) {
			this.s = s;
		}
		if (this.next != null) {
			this.next.setSource(s);
		}
	}

	// public TLog next() {
	// return this.next;
	// }

	@Override
	public void strOut(StringBuilder sb) {
		String mtype = "";
		switch (this.level) {
		case Error:
			mtype = this.format.error();
			break;
		case Warning:
			mtype = this.format.warning();
			break;
		case Notice:
			mtype = this.format.notice();
			break;
		case Info:
			mtype = this.format.notice();
			break;
		default:
			return;
		}
		sb.append("(");
		sb.append(Token.shortName(this.s.getPath()));
		sb.append(":");
		sb.append(this.s.linenum());
		sb.append("+");
		sb.append(this.s.column());
		sb.append(") [");
		sb.append(mtype);
		sb.append("] ");
		OStrings.appendFormat(sb, this.format, this.args);
		if (!this.s.isUnknownPosition()) {
			sb.append(System.lineSeparator());
			sb.append(this.s.line());
			sb.append(System.lineSeparator());
			sb.append(this.s.mark('^'));
		}
	}

	@Override
	public String toString() {
		return OStrings.stringfy(this);
	}

	public void emit(int level, Consumer<TLog> f) {
		for (TLog cur = this; cur != null; cur = cur.next) {
			if (cur.level <= level) {
				f.accept(cur);
			}
		}
	}

	public static void report(TLog log) {
		int level = log.level;
		String msg = log.toString();
		if ((level & Error) == Error) {
			OConsole.beginColor(OConsole.Red);
			msg = OConsole.bold(msg);
		} else if ((level & Notice) == Notice) {
			OConsole.beginColor(OConsole.Cyan);
		} else {
			OConsole.beginColor(OConsole.Magenta);
		}
		OConsole.println(msg);
		OConsole.endColor();
	}

	final static Token UnknownPosition = new Token("");

}

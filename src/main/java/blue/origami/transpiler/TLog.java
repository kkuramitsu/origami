package blue.origami.transpiler;

import blue.origami.nez.ast.LocaleFormat;
import blue.origami.nez.ast.SourcePosition;
import blue.origami.util.OConsole;
import blue.origami.util.OStrings;

public class TLog implements OStrings {
	public final static int Error = 1;
	public final static int Warning = 1 << 1;
	public final static int Notice = 1 << 2;
	public final static int Info = 1 << 3;
	public final static int Syntax = 1 << 4;
	public final static int Type = 1 << 5;

	public final static int TypeInfo = Info | Type;

	public SourcePosition s;
	public final int level;
	public final LocaleFormat format;
	public final Object[] args;
	public TLog prev = null;

	public TLog(TLog log, SourcePosition s, int level, LocaleFormat format, Object... args) {
		this.s = s == null ? SourcePosition.UnknownPosition : s;
		this.level = level;
		this.format = format;
		this.args = filter(args);
		this.prev = log;
	}

	public TLog(SourcePosition s, int level, LocaleFormat format, Object... args) {
		this(null, s, level, format, args);
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
		}
		return args;
	}

	public void setSource(SourcePosition s) {
		if (this.s == SourcePosition.UnknownPosition && s != null) {
			this.s = s;
		}
		if (this.prev != null) {
			this.prev.setSource(s);
		}
	}

	public TLog next() {
		return this.prev;
	}

	@Override
	public void strOut(StringBuilder sb) {
		String mtype = (this.level == Error ? this.format.error() : this.format.warning());
		if (this.level == Notice) {
			mtype = this.format.notice();
		}
		SourcePosition.appendFormatMessage(sb, this.s, mtype, this.format, this.args);
	}

	@Override
	public String toString() {
		return OStrings.stringfy(this);
	}

	public void dump() {
		for (TLog cur = this; cur != null; cur = cur.prev) {
			report(cur.level, cur.toString());
		}
	}

	static void report(int level, String msg) {
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

}

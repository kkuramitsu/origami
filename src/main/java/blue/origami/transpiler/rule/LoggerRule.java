package blue.origami.transpiler.rule;

import blue.origami.nez.ast.LocaleFormat;
import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TLog;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TLogCode;

public class LoggerRule {
	private TLog addMessage(TLog head, Tree<?> s, int level, LocaleFormat format, Object[] args) {
		return new TLog(head, s, level, format, args);
	}

	public TLog reportError(TLog log, Tree<?> s, LocaleFormat fmt, Object... args) {
		return this.addMessage(log, s, TLog.Error, fmt, args);
	}

	public TLog reportWarning(TLog log, Tree<?> s, LocaleFormat fmt, Object... args) {
		return this.addMessage(log, s, TLog.Warning, fmt, args);
	}

	public TLog reportNotice(TLog log, Tree<?> s, LocaleFormat fmt, Object... args) {
		return this.addMessage(log, s, TLog.Notice, fmt, args);
	}

	public TCode log(TLog log, TCode code) {
		if (log == null) {
			return code;
		}
		return new TLogCode(log, code);
	}

}

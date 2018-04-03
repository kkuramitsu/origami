package blue.origami.transpiler.rule;

import blue.origami.common.OFormat;
import blue.origami.common.TLog;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.LogCode;
import origami.nez2.Token;

public class LoggerRule {

	public TLog reportError(TLog head, Token s, OFormat fmt, Object... args) {
		return TLog.append(head, new TLog(s, TLog.Error, fmt, args));
	}

	public TLog reportWarning(TLog head, Token s, OFormat fmt, Object... args) {
		return TLog.append(head, new TLog(s, TLog.Error, fmt, args));
	}

	public TLog reportNotice(TLog head, Token s, OFormat fmt, Object... args) {
		return TLog.append(head, new TLog(s, TLog.Error, fmt, args));
	}

	public Code log(TLog head, Code code) {
		if (head == null) {
			return code;
		}
		return new LogCode(head, code);
	}

}

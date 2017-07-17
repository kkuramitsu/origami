package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.TCode;
import blue.origami.util.OLog;

public class LoggerRule {
	private OLog addMessage(OLog head, Tree<?> s, int level, String format, Object[] args) {
		OLog m = new OLog(s, level, format, args);
		if (head == null) {
			return m;
		}
		OLog cur = head;
		while (cur.next != null) {
			cur = cur.next;
		}
		cur.next = m;
		return head;
	}

	public OLog reportError(OLog log, Tree<?> s, String fmt, Object... args) {
		return this.addMessage(log, s, OLog.Error, fmt, args);
	}

	public OLog reportWarning(OLog log, Tree<?> s, String fmt, Object... args) {
		return this.addMessage(log, s, OLog.Warning, fmt, args);
	}

	public OLog reportNotice(OLog log, Tree<?> s, String fmt, Object... args) {
		return this.addMessage(log, s, OLog.Notice, fmt, args);
	}

	public TCode log(OLog log, TCode code) {
		if (log == null) {
			return code;
		}
		return code; // new WarningCode(code, head);
	}

}

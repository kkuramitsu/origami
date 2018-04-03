package blue.origami.transpiler.code;

import blue.origami.common.TLog;
import blue.origami.transpiler.CodeSection;
import origami.nez2.Token;

public class LogCode extends Code1 {
	protected TLog log;

	public LogCode(TLog log, Code inner) {
		super(AutoType, inner);
		this.log = log;
	}

	public TLog getLog() {
		return this.log;
	}

	@Override
	public Code setSource(Token s) {
		this.inner.setSource(s);
		this.log.setSource(s);
		return this;
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushLog(this);
	}

}
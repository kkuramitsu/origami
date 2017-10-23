package blue.origami.transpiler.code;

import blue.origami.common.OStrings;
import blue.origami.common.TLog;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;

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
	public Code setSource(AST t) {
		this.inner.setSource(t);
		this.log.setSource(t);
		return this;
	}

	@Override
	public void strOut(StringBuilder sb) {
		OStrings.append(sb, this.getInner());
	}

	@Override
	public void emitCode(Env env, CodeSection sec) {
		sec.pushLog(env, this);
	}

}
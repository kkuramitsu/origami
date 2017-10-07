package blue.origami.transpiler.code;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TLog;
import blue.origami.util.OStrings;

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
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushLog(env, this);
	}

}
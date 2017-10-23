package blue.origami.transpiler.code;

import blue.origami.common.OArrays;
import blue.origami.common.OFormat;
import blue.origami.common.OStrings;
import blue.origami.common.SourcePosition;
import blue.origami.common.TLog;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.type.Ty;

@SuppressWarnings("serial")
public class ErrorCode extends RuntimeException implements Code {

	private final TLog log;
	private final CommonCode dummy = new DoneCode();

	public ErrorCode(SourcePosition s, OFormat fmt, Object... args) {
		super();
		this.log = new TLog(s, TLog.Error, fmt, args);
	}

	public ErrorCode(OFormat fmt, Object... args) {
		this(SourcePosition.UnknownPosition, fmt, args);
	}

	public ErrorCode(Code at, OFormat fmt, Object... args) {
		super();
		this.log = new TLog(at.getSource(), TLog.Error, fmt, args);
	}

	// public ErrorCode(SourcePosition s, String fmt, Object... args) {
	// this(s, LocaleFormat.wrap(fmt), args);
	// }
	//
	// public ErrorCode(String fmt, Object... args) {
	// this(SourcePosition.UnknownPosition, LocaleFormat.wrap(fmt), args);
	// }

	@Override
	public boolean isGenerative() {
		return false;
	}

	@Override
	public boolean showError(Env env) {
		env.reportLog(this.log);
		return true;
	}

	public TLog getLog() {
		return this.log;
	}

	@Override
	public Code[] args() {
		return OArrays.emptyCodes;
	}

	@Override
	public Ty getType() {
		return this.dummy.getType();
	}

	@Override
	public Code asType(Env env, Ty ret) {
		this.dummy.setType(ret);
		return this;
	}

	@Override
	public AST getSource() {
		return this.dummy.getSource();
	}

	@Override
	public Code setSource(AST t) {
		this.dummy.setSource(t);
		this.log.setSource(t);
		return this;
	}

	@Override
	public void emitCode(Env env, CodeSection sec) {
		sec.pushError(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		OStrings.appendQuoted(sb, this.log);
	}

	@Override
	public void dumpCode(SyntaxHighlight sh) {
		sh.Error(this.log);
	}

	@Override
	public String toString() {
		return this.log.toString();
	}

}
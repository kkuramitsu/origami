package blue.origami.transpiler.code;

import blue.origami.nez.ast.LocaleFormat;
import blue.origami.nez.ast.SourcePosition;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TLog;
import blue.origami.transpiler.type.Ty;
import blue.origami.util.OStrings;

@SuppressWarnings("serial")
public class ErrorCode extends RuntimeException implements Code {

	private final TLog log;
	private final CommonCode dummy = new DoneCode();

	public ErrorCode(SourcePosition s, LocaleFormat fmt, Object... args) {
		super();
		this.log = new TLog(s, TLog.Error, fmt, args);
	}

	public ErrorCode(LocaleFormat fmt, Object... args) {
		this(SourcePosition.UnknownPosition, fmt, args);
	}

	public ErrorCode(Code at, LocaleFormat fmt, Object... args) {
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
	public boolean showError(TEnv env) {
		env.reportLog(this.log);
		return true;
	}

	public TLog getLog() {
		return this.log;
	}

	@Override
	public Code[] args() {
		return TArrays.emptyCodes;
	}

	@Override
	public Ty getType() {
		return this.dummy.getType();
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
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
	public void emitCode(TEnv env, CodeSection sec) {
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
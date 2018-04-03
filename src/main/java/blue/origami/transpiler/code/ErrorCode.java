package blue.origami.transpiler.code;

import blue.origami.common.OArrays;
import blue.origami.common.OFormat;
import blue.origami.common.SyntaxBuilder;
import blue.origami.common.TLog;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.type.Ty;
import origami.nez2.Token;

@SuppressWarnings("serial")
public class ErrorCode extends RuntimeException implements Code {

	private final TLog log;
	private final CommonCode dummy = new DoneCode();

	public ErrorCode(Token s, OFormat fmt, Object... args) {
		super();
		this.log = new TLog(s, TLog.Error, fmt, args);
	}

	public ErrorCode(OFormat fmt, Object... args) {
		this((Token) null, fmt, args);
	}

	public ErrorCode(Code at, OFormat fmt, Object... args) {
		super();
		this.log = new TLog(at.getSource(), TLog.Error, fmt, args);
	}

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
	public Token getSource() {
		return this.dummy.getSource();
	}

	@Override
	public Code setSource(Token s) {
		this.dummy.setSource(s);
		this.log.setSource(s);
		return this;
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushError(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.sexpr(sb, "error " + this.log);
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Error(this.log);
	}

	@Override
	public String toString() {
		return this.log.toString();
	}

}
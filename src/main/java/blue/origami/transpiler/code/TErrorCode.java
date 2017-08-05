package blue.origami.transpiler.code;

import blue.origami.nez.ast.LocaleFormat;
import blue.origami.nez.ast.SourcePosition;
import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TLog;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;
import blue.origami.util.OLog;
import blue.origami.util.StringCombinator;

@SuppressWarnings("serial")
public class TErrorCode extends RuntimeException implements TCode {

	private final TLog log;
	private final CommonCode dummy = new TDeclCode();

	public TErrorCode(SourcePosition s, LocaleFormat fmt, Object... args) {
		super();
		this.log = new TLog(s, OLog.Error, fmt, args);
	}

	public TErrorCode(LocaleFormat fmt, Object... args) {
		this(SourcePosition.UnknownPosition, fmt, args);
	}

	public TErrorCode(TCode at, LocaleFormat fmt, Object... args) {
		super();
		this.log = new TLog(at.getSource(), OLog.Error, fmt, args);
	}

	public TErrorCode(SourcePosition s, String fmt, Object... args) {
		this(s, LocaleFormat.wrap(fmt), args);
	}

	public TErrorCode(String fmt, Object... args) {
		this(SourcePosition.UnknownPosition, LocaleFormat.wrap(fmt), args);
	}

	public TLog getLog() {
		return this.log;
	}

	@Override
	public TCode[] args() {
		return TArrays.emptyCodes;
	}

	@Override
	public TType getType() {
		return this.dummy.getType();
	}

	@Override
	public TCode asType(TEnv env, TType t) {
		this.dummy.setType(t);
		return this;
	}

	@Override
	public Tree<?> getSource() {
		return this.dummy.getSource();
	}

	@Override
	public TCode setSource(Tree<?> t) {
		this.dummy.setSource(t);
		this.log.setSourcePosition(t);
		return this;
	}

	@Override
	public Template getTemplate(TEnv env) {
		return Template.Null;
	}

	@Override
	public String strOut(TEnv env) {
		return this.log.toString();
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushError(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		StringCombinator.appendQuoted(sb, this.log);
	}

}
package blue.origami.transpiler.code;

import blue.origami.nez.ast.LocaleFormat;
import blue.origami.nez.ast.SourcePosition;
import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TLog;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;
import blue.origami.util.OLog;

@SuppressWarnings("serial")
public class TErrorCode extends RuntimeException implements Code0 {

	private final TLog log;
	private TType ret;

	public TErrorCode(SourcePosition s, LocaleFormat fmt, Object... args) {
		super();
		this.log = new TLog(s, OLog.Error, fmt, args);
		this.ret = TType.tVoid;
	}

	public TErrorCode(LocaleFormat fmt, Object... args) {
		this(SourcePosition.UnknownPosition, fmt, args);
	}

	public TErrorCode(TCode at, LocaleFormat fmt, Object... args) {
		super();
		this.log = new TLog(null, OLog.Error, fmt, args);
		this.ret = TType.tVoid;
	}

	public TErrorCode(SourcePosition s, String fmt, Object... args) {
		this(s, LocaleFormat.wrap(fmt), args);
	}

	public TErrorCode(String fmt, Object... args) {
		this(SourcePosition.UnknownPosition, LocaleFormat.wrap(fmt), args);
	}

	@Override
	public TCode self() {
		return this;
	}

	public TLog getLog() {
		return this.log;
	}

	@Override
	public TType getType() {
		return this.ret;
	}

	@Override
	public TCode asType(TEnv env, TType t) {
		this.ret = t;
		return this;
	}

	public SourcePosition getSourcePosition() {
		return this.log.s;
	}

	@Override
	public TCode setSourcePosition(Tree<?> t) {
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

}
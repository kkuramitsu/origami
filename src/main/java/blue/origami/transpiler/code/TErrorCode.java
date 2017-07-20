package blue.origami.transpiler.code;

import blue.origami.nez.ast.LocaleFormat;
import blue.origami.nez.ast.SourcePosition;
import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TSkeleton;
import blue.origami.transpiler.TType;
import blue.origami.util.OLog;

@SuppressWarnings("serial")
public class TErrorCode extends RuntimeException implements TCode {

	private final OLog log;
	private TType ret;

	public TErrorCode(SourcePosition s, LocaleFormat fmt, Object... args) {
		super();
		this.log = new OLog(s, OLog.Error, fmt, args);
		this.ret = TType.tUnit;
	}

	public TErrorCode(LocaleFormat fmt, Object... args) {
		this(SourcePosition.UnknownPosition, fmt, args);
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

	public OLog getLog() {
		return this.log;
	}

	@Override
	public TType getType() {
		return this.ret;
	}

	public SourcePosition getSourcePosition() {
		return this.log.s;
	}

	@Override
	public TCode setSourcePosition(Tree<?> t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TSkeleton getTemplate(TEnv env) {
		return TSkeleton.Null;
	}

	@Override
	public String strOut(TEnv env) {
		return this.log.toString();
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushLog(this.log);
	}

}
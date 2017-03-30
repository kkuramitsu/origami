package blue.origami.ocode;

import blue.origami.lang.OEnv;
import blue.origami.nez.ast.LocaleFormat;
import blue.origami.nez.ast.SourcePosition;
import blue.origami.util.ODebug;
import blue.origami.util.OLog;

public class WarningCode extends OParamCode<OLog> implements OWrapperCode {

	public WarningCode(OCode node, int level, LocaleFormat fmt, Object... args) {
		super(new OLog(null, level, fmt, args), node.getType(), node);

	}

	public WarningCode(OCode node, LocaleFormat fmt, Object... args) {
		super(new OLog(null, OLog.Warning, fmt, args), node.getType(), node);

	}

	public WarningCode(OCode node, OLog m) {
		super(m, node.getType(), node);
	}

	@Override
	public OCode wrapped() {
		return this.getFirst();
	}

	@Override
	public void wrap(OCode code) {
		ODebug.NotAvailable(this);
	}

	public OLog getLog() {
		return this.getHandled();
	}

	@Override
	public OCode setSourcePosition(SourcePosition s) {
		super.setSourcePosition(s);
		this.getLog().setSourcePosition(s);
		return this;
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		OLog.report(env, this.getLog());
		return this.getParams()[0].eval(env);
	}

	@Override
	public void generate(OGenerator gen) {
		gen.pushWarning(this);
	}

}
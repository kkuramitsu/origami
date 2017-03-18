package origami.code;

import origami.OEnv;
import origami.OLog;
import origami.asm.OAsm;
import origami.nez.ast.SourcePosition;
import origami.type.OType;

public class OWarningCode extends OParamCode<OLog> {

	public OWarningCode(OCode node, int level, String fmt, Object... args) {
		super(new OLog(null, level, fmt, args), node.getType(), node);

	}

	public OWarningCode(OCode node, String fmt, Object... args) {
		super(new OLog(null, OLog.Warning, fmt, args), node.getType(), node);

	}

	public OWarningCode(OCode node, OLog m) {
		super(m, node.getType(), node);
	}

	@Override
	public int getMatchCost() {
		return this.getParams()[0].getMatchCost();
	}

	@Override
	public OCode refineType(OEnv env, OType ty) {
		this.nodes[0] = this.nodes[0].refineType(env, ty);
		return this;
	}

	@Override
	public boolean hasReturnCode() {
		return nodes[0].hasReturnCode();
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
		OLog.report(env, this.getHandled());
		return this.getParams()[0].eval(env);
	}

	@Override
	public void generate(OAsm gen) {
		gen.pushWarning(this);
	}

}
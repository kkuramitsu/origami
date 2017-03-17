package origami.code;

import origami.OEnv;
import origami.OLog;
import origami.asm.OAsm;
import origami.type.OType;

public class OWarningCode extends MessageCode {

	public OWarningCode(OCode node, int level, String fmt, Object... args) {
		super(level, new OLog(null, OLog.Warning, fmt, args), node.getType(), node);

	}

	public OWarningCode(OCode node, String fmt, Object... args) {
		super(OLog.Warning, new OLog(null, OLog.Warning, fmt, args), node.getType(), node);

	}

	public OWarningCode(OCode node, OLog m) {
		super(OLog.Warning, m, node.getType(), node);
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

	@Override
	public OCode checkAcc(OEnv env) {
		return nodes[0].checkAcc(env);
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
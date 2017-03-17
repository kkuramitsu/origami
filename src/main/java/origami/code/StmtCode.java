package origami.code;

import origami.OEnv;
import origami.asm.OAsm;

public abstract class StmtCode extends OParamCode<String> {

	public StmtCode(OEnv env, String name, OCode... nodes) {
		super(name, env.t(void.class)/* OType.Unit */, nodes);
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		throw new RuntimeException(this.getHandled() + " is not executable");
		// for (OCode node : nodes) {
		// node.eval(env);
		// }
		// return null;
	}

	@Override
	public void generate(OAsm gen) {
		// gen.pushStmt(this);
	}

}
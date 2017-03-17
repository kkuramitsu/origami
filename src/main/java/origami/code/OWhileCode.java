package origami.code;

import origami.OEnv;
import origami.asm.OAsm;

public class OWhileCode extends OParamCode<Void> {

	public OWhileCode(OEnv env, OCode condCode, OCode bodyCode) {
		super(null, env.t(void.class), condCode, bodyCode);
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		while ((Boolean) this.getParams()[0].eval(env)) {
			this.getParams()[1].eval(env);
		}
		return null;
	}

	@Override
	public void generate(OAsm gen) {
		gen.pushLoop(this);
	}

	public OCode cond() {
		return nodes[0];
	}

	public OCode body() {
		return nodes[1];
	}
}
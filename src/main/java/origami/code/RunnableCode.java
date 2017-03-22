package origami.code;

import origami.OEnv;
import origami.asm.OAsm;

public class RunnableCode extends OParamCode<Runnable> {

	public RunnableCode(OEnv env, Runnable handled) {
		super(handled, env.t(void.class));
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		this.getHandled().run();
		return null;
	}

	@Override
	public void generate(OGenerator gen) {

	}

}

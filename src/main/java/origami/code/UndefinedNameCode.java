package origami.code;

import origami.OEnv;
import origami.asm.OAsm;
import origami.ffi.OrigamiException;

public class UndefinedNameCode extends OErrorCode {
	private UndefinedNameCode(OEnv env, int cost) {
		super(env, null, env.t(Object.class)/* OType.Object */, null, cost);
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		throw new OrigamiException("undefined");
	}

	@Override
	public void generate(OAsm gen) {

	}
}
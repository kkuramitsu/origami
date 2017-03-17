package origami.code;

import java.lang.reflect.Array;

import origami.OEnv;
import origami.asm.OAsm;
import origami.lang.OMethodHandle;

public class GetSizeCode extends OMethodCode {
	public GetSizeCode(OEnv env, OMethodHandle m, OCode expr) {
		super(m, env.t(int.class), new OCode[] { expr }, 0);
	}

	@Override
	public void generate(OAsm gen) {
		gen.pushGetSize(this);
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		if (this.getMethod() != null) {
			super.eval(env);
		}
		Object[] values = evalParams(env, nodes);
		return Array.getLength(values[0]);
	}
}
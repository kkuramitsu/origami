package origami.code;

import java.lang.reflect.Array;

import origami.OEnv;
import origami.asm.OAsm;
import origami.lang.OMethodHandle;
import origami.type.OType;

/**
 * <pre>
 * Params 0 : Receiver (OCode) 1 : Index (OCode) 2 : Expression (OCode)
 **/
public class SetIndexCode extends OMethodCode {
	public SetIndexCode(OType ret, OMethodHandle m, int matchCost, OCode... nodes) {
		super(m, ret, nodes, matchCost);
	}

	@Override
	public void generate(OGenerator gen) {
		gen.pushSetIndex(this);
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		if (this.getMethod() != null) {
			super.eval(env);
		}
		Object[] values = evalParams(env, this.getParams());
		Array.set(values[0], (Integer) values[1], values[2]);
		return values[2];
	}

}

package origami.code;

import java.lang.reflect.Array;

import origami.OEnv;
import origami.asm.OAsm;
import origami.lang.OMethodHandle;
import origami.type.OType;

/**
 * <pre>
 * Params 0 : Receiver (OCode) 1 : Index (OCode)
 **/

public class GetIndexCode extends OMethodCode {
	public GetIndexCode(OType ret, OMethodHandle m, int matchCost, OCode... nodes) {
		super(m, ret, nodes, matchCost);
	}

	@Override
	public void generate(OAsm gen) {
		gen.pushGetIndex(this);
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		if (this.getMethod() != null) {
			super.eval(env);
		}
		Object[] values = evalParams(env, nodes);
		return Array.get(values[0], (Integer) values[1]);
	}

	// @Override
	// public OCode newAssignCode(OEnv env, OType type, OCode right) {
	// OMethodHandle m = this.getMethod();
	// if (m == null) {
	// return new SetIndexCode(env.t(void.class), null, 0, nodes[0], nodes[1],
	// right);
	// }
	// String name = m.getLocalName().replace("get", "set");
	// OCode r = nodes[0].newMethodCode(env, name, nodes[1], right);
	// if (r instanceof MethodCode) {
	// ((MethodCode) r).getMethod();
	// }
	// }

}
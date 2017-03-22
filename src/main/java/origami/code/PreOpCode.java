package origami.code;

import origami.OEnv;
import origami.asm.OAsm;
import origami.asm.code.DupCode;
import origami.type.OType;

public class PreOpCode extends OParamCode<String> {
	OCode setter;

	public PreOpCode(String handled, OType returnType, OCode left, OCode expr, OEnv env) {
		super(handled, returnType);
		OCode op = new OMultiCode(left.newBinaryCode(env, handled, expr), new DupCode(left));
		this.setter = left.newAssignCode(env, left.getType(), op);
	}

	@Override
	public void generate(OGenerator gen) {
		this.setter.generate(gen);
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		return setter.eval(env);
	}

	public OCode setter() {
		return this.setter;
	}
}

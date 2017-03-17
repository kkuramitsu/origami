package origami.code;

import origami.OEnv;
import origami.asm.OAsm;

public class NestedEnvCode extends OParamCode<OEnv> {
	private final OCode blockCode;

	public NestedEnvCode(OEnv env, OCode code) {
		super(env, code.getType());
		this.blockCode = code;
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		return blockCode.eval(this.getHandled());
	}

	@Override
	public void generate(OAsm gen) {
		blockCode.generate(gen);
	}

}

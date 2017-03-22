package origami.code;

import origami.OEnv;

public class NestedEnvCode extends OParamCode<OEnv> {
	private final OCode blockCode;

	public NestedEnvCode(OEnv env, OCode code) {
		super(env, code.getType());
		this.blockCode = code;
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		return this.blockCode.eval(this.getHandled());
	}

	@Override
	public void generate(OGenerator gen) {
		this.blockCode.generate(gen);
	}

}

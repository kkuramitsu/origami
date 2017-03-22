package origami.code;

import origami.OEnv;
import origami.util.OScriptUtils;

public class OWhileCode extends OParamCode<Void> {

	public OWhileCode(OEnv env, OCode condCode, OCode nextCode, OCode bodyCode) {
		super(null, env.t(void.class), condCode, nextCode, bodyCode);
	}

	public OWhileCode(OEnv env, OCode condCode, OCode bodyCode) {
		super(null, env.t(void.class), condCode, new OEmptyCode(env), bodyCode);
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		return OScriptUtils.eval(env, this);
	}

	@Override
	public void generate(OGenerator gen) {
		gen.pushWhile(this);
	}

	public OCode condCode() {
		return this.nodes[0];
	}

	public OCode nextCode() {
		return this.nodes[1];
	}

	public OCode bodyCode() {
		return this.nodes[2];
	}
}
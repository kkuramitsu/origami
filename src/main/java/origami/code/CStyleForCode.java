package origami.code;

import origami.OEnv;

public class CStyleForCode extends OSugarCode {

	public CStyleForCode(OEnv env, OCode initCode, OCode condCode, OCode nextCode, OCode bodyCode) {
		super(env, env.t(void.class), initCode, condCode, nextCode, bodyCode);
	}

	public OCode initCode() {
		return this.nodes[0];
	}

	public OCode condCode() {
		return this.nodes[1];
	}

	public OCode nextCode() {
		return this.nodes[2];
	}

	public OCode bodyCode() {
		return this.nodes[3];
	}

	@Override
	public OCode desugar() {
		OCode whileCode = new OWhileCode(this.env(), this.condCode(), this.nextCode(), this.bodyCode());
		return new OMultiCode(this.initCode(), whileCode);
	}
}

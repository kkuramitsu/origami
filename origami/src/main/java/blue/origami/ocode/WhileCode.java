package blue.origami.ocode;

import blue.origami.lang.OEnv;
import blue.origami.rule.ScriptAnalysis;

public class WhileCode extends OParamCode<Void> {

	public WhileCode(OEnv env, OCode condCode, OCode nextCode, OCode bodyCode) {
		super(null, env.t(void.class), condCode, nextCode, bodyCode);
	}

	public WhileCode(OEnv env, OCode condCode, OCode bodyCode) {
		super(null, env.t(void.class), condCode, new EmptyCode(env), bodyCode);
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		return ScriptAnalysis.eval(env, this);
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
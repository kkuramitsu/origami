package origami.code;

import origami.OEnv;
import origami.type.OType;

public class PostOpCode extends OParamCode<String> {
	public PostOpCode(String handled, OType returnType, OCode... nodes) {
		super(handled, returnType, nodes);
	}

	@Override
	public void generate(OGenerator gen) {
		this.expr().generate(gen);
		this.setter().generate(gen);
		// gen.mBuilder.pop();
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		Object v = this.expr().eval(env);
		this.setter().eval(env);
		return v;
	}

	public OCode expr() {
		return this.nodes[0];
	}

	public OCode setter() {
		return this.nodes[1];
	}
}

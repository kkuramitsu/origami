package origami.code;

import origami.OEnv;
import origami.asm.OAsm;
import origami.type.OType;

public class PostOpCode extends OParamCode<String> {
	public PostOpCode(String handled, OType returnType, OCode... nodes) {
		super(handled, returnType, nodes);
	}

	@Override
	public void generate(OAsm gen) {
		this.expr().generate(gen);
		this.setter().generate(gen);
		// gen.mBuilder.pop();
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		Object v = expr().eval(env);
		setter().eval(env);
		return v;
	}

	public OCode expr() {
		return nodes[0];
	}

	public OCode setter() {
		return nodes[1];
	}
}

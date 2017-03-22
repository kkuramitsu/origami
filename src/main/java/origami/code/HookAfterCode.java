package origami.code;

import origami.lang.OEnv;
import origami.type.OType;

public class HookAfterCode extends OMultiCode {

	public HookAfterCode(OCode left, OCode hook) {
		super(0, left, hook);
	}

	@Override
	public OType getType() {
		return this.getFirst().getType();
	}

	@Override
	public OCode refineType(OEnv env, OType t) {
		this.getFirst().refineType(env, t);
		return this;
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		Object v = this.getFirst().eval(env);
		for (int i = 1; i < this.nodes.length; i++) {
			nodes[i].eval(env);
		}
		return v;
	}

}
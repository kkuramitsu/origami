package blue.origami.rule.kal;

import blue.origami.lang.OEnv;
import blue.origami.lang.type.OType;
import blue.origami.ocode.OCode;
import blue.origami.ocode.MultiCode;

public class HookAfterCode extends MultiCode {

	public HookAfterCode(OCode left, OCode hook) {
		super(left, hook);
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
			this.nodes[i].eval(env);
		}
		return v;
	}

}

package origami.code;

import origami.OEnv;
import origami.asm.OAsm;
import origami.lang.OMethodHandle;
import origami.lang.callsite.OFuncCallSite;
import origami.type.OFuncType;

public class FuncNameCode extends OParamCode<String> {
	OEnv env;
	final OMethodHandle mh;

	public FuncNameCode(OEnv env, String name, OMethodHandle mh) {
		super(name, env.t(void.class));
		this.env = env;
		this.mh = mh;
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		return OFuncType.newFuncCode(this.env, mh).eval(env);
	}

	@Override
	public void generate(OGenerator gen) {
		OFuncType.newFuncCode(this.env, mh).generate(gen);
	}

	@Override
	public OCode newApplyCode(OEnv env, OCode... params) {
		String name = this.getHandled();
		return env.get(OFuncCallSite.class).findParamCode(env, name, params);
	}

}
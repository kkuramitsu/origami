package origami.code;

import origami.OEnv;
import origami.ffi.OCast;
import origami.lang.OConv.OConvCallSite;
import origami.lang.ODynamicMethodHandle;
import origami.lang.OMethodHandle;
import origami.type.OType;

public class DynamicCastCode extends OCastCode {
	private OEnv env;

	public DynamicCastCode(OEnv env, OType t, OCode node) {
		super(t, node.isUntyped() ? OCast.UPCAST : OCast.ANYCAST, node);
		this.env = env;
	}

	@Override
	public OCode retypeLocal() {
		if (this.getFirst().isUntyped()) {
			return this.getFirst().asType(env, this.getType());
		}
		return this;
	}

	@Override
	public OMethodHandle getMethod() {
		return new ODynamicMethodHandle(env, env.get(OConvCallSite.class), "<conv>", this.getType(), 1);
	}
}
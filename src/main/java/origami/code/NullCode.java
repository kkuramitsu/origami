package origami.code;

import origami.OEnv;
import origami.type.OType;
import origami.type.OUntypedType;

public class NullCode extends OValueCode {

	public NullCode(OType ret) {
		super(null, ret);
	}

	public NullCode(OEnv env) {
		this(env.t(OUntypedType.class));
	}

	@Override
	public OCode refineType(OEnv env, OType ty) {
		if (this.getType() instanceof OUntypedCode) {
			setType(ty);
		}
		return this;
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		return this.getType().getDefaultValue();
	}

}

package origami.code;

import java.lang.reflect.Constructor;

import origami.OEnv;
import origami.ffi.OCast;
import origami.lang.OConstructor;
import origami.lang.OMethodHandle;
import origami.type.OType;

public class ClassInitCode extends OMethodCode {

	public ClassInitCode(OMethodHandle method, OCode... nodes) {
		super(method, nodes, OCast.SAME);
		assert (method.isSpecial());
	}

	public ClassInitCode(OEnv env, Constructor<?> c, OCode... nodes) {
		super(new OConstructor(env, c), nodes, OCast.SAME);
	}

	@Override
	public OType getType() {
		return nodes[0].getTypeSystem().newType(void.class);
	}

}

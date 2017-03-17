package origami.rule;

import origami.OEnv;
import origami.lang.OTypeName;
import origami.type.OType;
import origami.type.OUntypedType;

public class OConfig {

	/* Package */

	/* Type Configuration */

	public static OType Untyped(OEnv env) {
		return env.t(OUntypedType.class);
	}

	public static OType declType(OEnv env, OType t) {
		return env.t(void.class);
	}

	public static OType assignedType(OEnv env, OType t) {
		return env.t(void.class);
	}

	public static OType inferParamType(OEnv env, String funcname, String paramname, OType ty) {
		return OTypeName.lookupTypeName(env, paramname);
	}

	public static OType inferReturnType(OEnv env, String funcname, OType ret) {
		return ret;
	}

	// /* DefaultValue */
	//
	// public static Object defaultValue(OType t) {
	// return t.getDefaultValue();
	// }
	//
	// public static Object defaultValue(OEnv env, OType t) {
	// return t.getDefaultValue();
	// }

	/* Message Configuration */

	public static void println(OEnv env, Object o) {
		System.out.println(o);
	}

}

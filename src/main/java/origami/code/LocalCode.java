package origami.code;

import origami.ODebug;
import origami.OEnv;
import origami.asm.OAsm;
import origami.type.OType;

public class LocalCode<T> extends OParamCode<T> {

	protected LocalCode(T handled, OType ret, OCode... codes) {
		super(handled, ret, codes);
	}

	protected LocalCode(T handled, OType ret) {
		super(handled, ret);
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		ODebug.FIXME("Don't eval");
		return null;
	}

	@Override
	public void generate(OAsm gen) {
		ODebug.FIXME("Don't generate directly");
	}

}

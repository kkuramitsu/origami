package origami.code;

import origami.asm.OAsm;
import origami.type.OType;

public class ThisCode extends OValueCode {
	public ThisCode(OType ty) {
		super(null, ty);
	}

	@Override
	public void generate(OAsm gen) {
		gen.pushThis();
	}

}

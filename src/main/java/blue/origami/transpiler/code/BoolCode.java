package blue.origami.transpiler.code;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.type.Ty;

public class BoolCode extends CommonCode implements ValueCode {
	private boolean value;
	public final static BoolCode True = new BoolCode(true);
	public final static BoolCode False = new BoolCode(false);

	public BoolCode(boolean value) {
		super(Ty.tBool);
		this.value = value;
	}

	@Override
	public Object getValue() {
		return this.value;
	}

	public boolean isTrue() {
		return this.value;
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushBool(this);
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Literal(this.value);
	}

}

package blue.origami.transpiler.code;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.type.Ty;

public class DoubleCode extends CommonCode implements ValueCode {
	private double value;

	public DoubleCode(double value) {
		super(Ty.tFloat);
		this.value = value;
	}

	@Override
	public Object getValue() {
		return this.value;
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushDouble(this);
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Literal(this.value);
	}

}
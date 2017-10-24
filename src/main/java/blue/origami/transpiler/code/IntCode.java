package blue.origami.transpiler.code;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.type.Ty;

public class IntCode extends CommonCode implements ValueCode {
	private int value;

	public IntCode(int value) {
		super(Ty.tInt);
		this.value = value;
	}

	@Override
	public Object getValue() {
		return this.value;
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushInt(this);
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Literal(this.value);
	}

	public static class TCharCode extends IntCode {

		public TCharCode(int value) {
			super(value);
			this.setType(Ty.tChar);
		}

	}
}

package blue.origami.transpiler.code;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.type.Ty;

public class CharCode extends CommonCode implements ValueCode {
	private char value;

	public CharCode(char value) {
		super(Ty.tChar);
		this.value = value;
	}

	@Override
	public Object getValue() {
		return this.value;
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushChar(this);
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Literal(this.value);
	}
}

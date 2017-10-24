package blue.origami.transpiler.code;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.type.Ty;

public class StringCode extends CommonCode implements ValueCode {
	private String value;

	public StringCode(String value) {
		super(Ty.tString);
		this.value = value;
	}

	@Override
	public Object getValue() {
		return this.value;
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushString(this);
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.StringLiteral(this.value);
	}

}
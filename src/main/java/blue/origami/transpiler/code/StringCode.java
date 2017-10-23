package blue.origami.transpiler.code;

import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
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
	public void emitCode(Env env, CodeSection sec) {
		sec.pushString(env, this);
	}

	@Override
	public void dumpCode(SyntaxHighlight sh) {
		sh.StringLiteral(this.value);
	}

}
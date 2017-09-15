package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
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
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushString(env, this);
	}

	@Override
	public void dumpCode(SyntaxHighlight sh) {
		sh.StringLiteral(this.value);
	}

}
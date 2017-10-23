package blue.origami.transpiler.code;

import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.type.Ty;

public class BoolCode extends CommonCode implements ValueCode {
	private boolean value;

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
	public void emitCode(Env env, CodeSection sec) {
		sec.pushBool(env, this);
	}

	@Override
	public void dumpCode(SyntaxHighlight sh) {
		sh.Literal(this.value);
	}

}

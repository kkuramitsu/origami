package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
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
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushInt(env, this);
	}

	public static class TCharCode extends IntCode {

		public TCharCode(int value) {
			super(value);
			this.setType(Ty.tChar);
		}

	}
}

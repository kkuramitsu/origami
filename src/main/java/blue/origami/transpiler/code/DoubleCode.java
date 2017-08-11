package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Ty;

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
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushDouble(env, this);
	}

}
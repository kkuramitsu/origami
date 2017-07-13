package blue.origami.transpiler.code;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TTemplate;
import blue.origami.transpiler.TType;

public class TDoubleCode extends TTypedCode {
	private double value;

	public TDoubleCode(double value) {
		super(TType.tFloat);
		this.value = value;
	}

	@Override
	public TTemplate getTemplate(TEnv env) {
		return env.get("0:Float", TTemplate.class);
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.value);
	}

}
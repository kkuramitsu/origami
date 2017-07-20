package blue.origami.transpiler.code;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TSkeleton;
import blue.origami.transpiler.TType;

public class TTypeCode extends TTypedCode {
	private TType value;

	public TTypeCode(TType value) {
		super(TType.tUnit);
		this.value = value;
	}

	public TType getTypeValue() {
		return this.value;
	}

	@Override
	public TSkeleton getTemplate(TEnv env) {
		return env.get(this.value.toString(), TSkeleton.class);
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.value);
	}

}

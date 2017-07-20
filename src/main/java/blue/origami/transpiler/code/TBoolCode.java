package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TSkeleton;
import blue.origami.transpiler.TType;

public class TBoolCode extends TTypedCode implements TValueCode {
	private boolean value;

	TBoolCode(boolean value) {
		super(TType.tBool);
		this.value = value;
	}

	@Override
	public Object getValue() {
		return this.getValue();
	}

	@Override
	public TSkeleton getTemplate(TEnv env) {
		return env.get(this.value ? "true:Bool" : "false:Bool", TSkeleton.class);
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.value);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushBool(this);
	}

}

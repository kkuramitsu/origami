package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TTemplate;
import blue.origami.transpiler.TType;

class TBoolCode extends TTypedCode {
	private boolean value;

	TBoolCode(boolean value) {
		super(TType.tBool);
		this.value = value;
	}

	@Override
	public TTemplate getTemplate(TEnv env) {
		return env.get(this.value ? "true:Bool" : "false:Bool", TTemplate.class);
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.value);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.push(this.strOut(env));
	}

}
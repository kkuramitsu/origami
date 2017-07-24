package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;

public class TBoolCode extends EmptyTypedCode implements TValueCode {
	private boolean value;

	public TBoolCode(boolean value) {
		super(TType.tBool);
		this.value = value;
	}

	@Override
	public Object getValue() {
		return this.getValue();
	}

	@Override
	public Template getTemplate(TEnv env) {
		return env.get(this.value ? "true:Bool" : "false:Bool", Template.class);
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.value);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushBool(env, this);
	}

}

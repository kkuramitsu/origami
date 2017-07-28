package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;

public class TStringCode extends EmptyTypedCode implements TValueCode {
	private String value;

	public TStringCode(String value) {
		super(TType.tString);
		this.value = value;
	}

	@Override
	public Object getValue() {
		return this.value;
	}

	@Override
	public Template getTemplate(TEnv env) {
		return env.getTemplate("0:String", "\"%s\"");
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.value);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushString(env, this);
	}

}
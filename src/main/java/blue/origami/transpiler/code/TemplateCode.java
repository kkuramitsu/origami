package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;

public class TemplateCode extends TypedCodeN {

	public TemplateCode(TCode... codes) {
		super(TType.tString, null, codes);
	}

	@Override
	public Template getTemplate(TEnv env) {
		return env.getTemplate("String", "\"%s\"");
	}

	@Override
	public String strOut(TEnv env) {
		return "TODO"; // this.getTemplate(env).format(this.value);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushTemplate(env, this);
	}

}
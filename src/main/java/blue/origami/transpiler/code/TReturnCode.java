package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Template;

public class TReturnCode extends SingleCode {

	public TReturnCode(TCode expr) {
		super(expr);
	}

	@Override
	public boolean hasReturn() {
		return true;
	}

	@Override
	public TCode addReturn() {
		return this;
	}

	@Override
	public Template getTemplate(TEnv env) {
		return env.getTemplate("return", "%s");
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushReturn(env, this);
	}

}

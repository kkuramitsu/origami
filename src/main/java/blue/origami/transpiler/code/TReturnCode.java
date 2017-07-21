package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TSkeleton;

public class TReturnCode extends SingleCode {

	public TReturnCode(TCode expr) {
		super(expr);
	}

	@Override
	public TSkeleton getTemplate(TEnv env) {
		return env.getTemplate("return", "%s");
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushReturn(env, this);
	}

}

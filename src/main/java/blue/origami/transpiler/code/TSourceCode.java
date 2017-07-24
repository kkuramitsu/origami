package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;

public class TSourceCode extends TMultiCode {

	public TSourceCode(TCode... args) {
		super(args);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		for (TCode a : this.args) {
			a.emitCode(env, sec);
		}
	}
}
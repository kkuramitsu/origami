package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;

public class SourceCode extends MultiCode {

	public SourceCode(Code... args) {
		super(args);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		for (Code a : this.args) {
			if (!a.showError(env)) {
				a.emitCode(env, sec);
			}
		}
	}
}
package blue.origami.transpiler.code;

import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;

public class SourceCode extends MultiCode {

	private final Env env;

	public SourceCode(Env env, Code... args) {
		super(args);
		this.env = env;
	}

	@Override
	public void emitCode(CodeSection sec) {
		for (Code a : this.args) {
			if (!a.showError(this.env)) {
				a.emitCode(sec);
			}
		}
	}
}
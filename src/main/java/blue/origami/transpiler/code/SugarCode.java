package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Ty;
import blue.origami.util.ODebug;

public class SugarCode extends CommonCode {

	public SugarCode() {
		super();
	}

	protected Code[] makeArgs(Code... args) {
		return args;
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		this.asType(env, Ty.tUntyped()).emitCode(env, sec);
	}

	@Override
	public void strOut(StringBuilder sb) {
		ODebug.TODO();
	}

}
package blue.origami.transpiler.code;

import blue.origami.common.ODebug;
import blue.origami.transpiler.CodeSection;

public class SugarCode extends CommonCode {

	public SugarCode() {
		super();
	}

	protected Code[] makeArgs(Code... args) {
		return args;
	}

	@Override
	public void emitCode(CodeSection sec) {
		ODebug.TODO();
		// this.asType(env, Ty.tUntyped()).emitCode(sec);
	}

}
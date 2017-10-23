package blue.origami.transpiler.code;

import blue.origami.common.ODebug;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.type.Ty;

public class SugarCode extends CommonCode {

	public SugarCode() {
		super();
	}

	protected Code[] makeArgs(Code... args) {
		return args;
	}

	@Override
	public void emitCode(Env env, CodeSection sec) {
		this.asType(env, Ty.tUntyped()).emitCode(env, sec);
	}

	@Override
	public void strOut(StringBuilder sb) {
		ODebug.TODO();
	}

}
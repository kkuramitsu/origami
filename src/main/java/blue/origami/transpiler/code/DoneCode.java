package blue.origami.transpiler.code;

import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.type.Ty;

public class DoneCode extends CommonCode {

	public DoneCode() {
		super(Ty.tVoid);
	}

	@Override
	public boolean isGenerative() {
		return false;
	}

	@Override
	public void emitCode(TEnv env, CodeSection sec) {
		// do nothing
	}

	@Override
	public void strOut(StringBuilder sb) {
	}

	@Override
	public void dumpCode(SyntaxHighlight sh) {
	}

}

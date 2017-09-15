package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.type.Ty;

public class DeclCode extends CommonCode {

	public DeclCode() {
		super(Ty.tVoid);
	}

	@Override
	public boolean isGenerative() {
		return false;
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		// do nothing
	}

	@Override
	public void strOut(StringBuilder sb) {
	}

	@Override
	public void dumpCode(SyntaxHighlight sh) {
	}

}

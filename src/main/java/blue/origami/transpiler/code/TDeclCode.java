package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Ty;

public class TDeclCode extends CommonCode {

	public TDeclCode() {
		super(Ty.tVoid);
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public String strOut(TEnv env) {
		return "";
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		// do nothing
	}

	@Override
	public void strOut(StringBuilder sb) {
	}

}

package blue.origami.transpiler.code;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
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
	public void emitCode(CodeSection sec) {
		// do nothing
	}

	@Override
	public void strOut(StringBuilder sb) {
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
	}

}

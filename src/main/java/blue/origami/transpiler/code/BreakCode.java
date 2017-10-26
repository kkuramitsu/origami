package blue.origami.transpiler.code;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.type.Ty;

public class BreakCode extends CommonCode {

	public BreakCode() {
		super(Ty.tVoid);
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushBreak(this);
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Keyword("break");
	}

}
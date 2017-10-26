package blue.origami.transpiler.code;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;

public class ThrowCode extends Code1 {

	public ThrowCode(Code expr) {
		super(AutoType, expr);
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushThrow(this);
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Keyword("throw");
		sh.s();
		sh.Expr(this.getInner());
	}

}
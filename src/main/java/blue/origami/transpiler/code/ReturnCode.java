package blue.origami.transpiler.code;

import blue.origami.common.OStrings;
import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;

public class ReturnCode extends Code1 {

	public ReturnCode(Code expr) {
		super(AutoType, expr);
	}

	@Override
	public boolean hasReturn() {
		return true;
	}

	@Override
	public Code addReturn() {
		return this;
	}

	@Override
	public void emitCode(Env env, CodeSection sec) {
		sec.pushReturn(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		OStrings.append(sb, this.getInner());
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Expr(this.getInner());
	}

}

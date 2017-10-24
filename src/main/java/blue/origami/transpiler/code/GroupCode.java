package blue.origami.transpiler.code;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.type.Ty;

public class GroupCode extends Code1 {
	GroupCode(Code inner) {
		super(AutoType, inner);
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushGroup(this);
	}

	@Override
	public Code asType(Env env, Ty ret) {
		this.inner.asType(env, ret);
		return this;
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Token("(");
		sh.Expr(this.getInner());
		sh.Token(")");
	}

}

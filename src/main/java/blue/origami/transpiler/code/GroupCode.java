package blue.origami.transpiler.code;

import blue.origami.common.OStrings;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.type.Ty;

public class GroupCode extends Code1 {
	GroupCode(Code inner) {
		super(AutoType, inner);
	}

	@Override
	public void emitCode(Env env, CodeSection sec) {
		sec.pushGroup(env, this);
	}

	@Override
	public Code asType(Env env, Ty ret) {
		this.inner.asType(env, ret);
		return this;
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("(");
		OStrings.append(sb, this.getInner());
		sb.append(")");
	}

	@Override
	public void dumpCode(SyntaxHighlight sh) {
		sh.Token("(");
		sh.Expr(this.getInner());
		sh.Token(")");
	}

}

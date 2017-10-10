package blue.origami.transpiler.code;

import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.type.Ty;
import blue.origami.util.OStrings;

public class GroupCode extends Code1 {
	GroupCode(Code inner) {
		super(AutoType, inner);
	}

	@Override
	public void emitCode(TEnv env, CodeSection sec) {
		sec.pushGroup(env, this);
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
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

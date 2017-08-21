package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.type.Ty;
import blue.origami.util.StringCombinator;

public class GroupCode extends Code1 {
	GroupCode(Code inner) {
		super(AutoType, inner);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushGroup(env, this);
	}

	public Code asType(TEnv env, Ty ret) {
		this.inner.asType(env, ret);
		return this;
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("(");
		StringCombinator.append(sb, this.getInner());
		sb.append(")");
	}

}

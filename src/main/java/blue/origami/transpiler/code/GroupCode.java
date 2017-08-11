package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.util.StringCombinator;

public class GroupCode extends Code1 {
	GroupCode(Code inner) {
		super(AutoType, inner);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		// sec.push(this.strOut(env));
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("(");
		StringCombinator.append(sb, this.getInner());
		sb.append(")");
	}

}

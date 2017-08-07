package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Template;
import blue.origami.util.StringCombinator;

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
	public Template getTemplate(TEnv env) {
		return env.getTemplate("return", "%s");
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushReturn(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		StringCombinator.append(sb, this.getInner());
	}

}

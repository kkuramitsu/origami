package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Template;

public class TGroupCode extends Code1 {
	TGroupCode(TCode inner) {
		super(inner);
	}

	@Override
	public Template getTemplate(TEnv env) {
		Template t = env.get("()", Template.class);
		return (t == null) ? Template.Null : t;
	}

	@Override
	public String strOut(TEnv env) {
		Template t = env.get("()", Template.class);
		if (t == null) {
			return this.inner.strOut(env);
		}
		return t.format(this.inner.strOut(env));
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.push(this.strOut(env));
	}

}

package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;
import blue.origami.util.StringCombinator;

public class TLetCode extends Code1 {
	private TType decltype;
	private String name;

	public TLetCode(String name, TType type, TCode expr) {
		super(expr);
		this.name = name;
		this.decltype = type;
	}

	public String getName() {
		return this.name;
	}

	public TType getDeclType() {
		return this.decltype;
	}

	@Override
	public Template getTemplate(TEnv env) {
		return env.getTemplate("let", "%2$s=%3$s");
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.decltype.strOut(env), this.name, this.getInner().strOut(env));
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushLet(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.name);
		sb.append(" = ");
		StringCombinator.append(sb, this.getInner());
	}

}

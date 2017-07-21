package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TSkeleton;
import blue.origami.transpiler.TType;

public class TLetCode extends TStaticAtomCode {
	private TType decltype;
	private String name;
	private TCode expr;

	public TLetCode(String name, TType type, TCode expr) {
		super(TType.tVoid);
		this.name = name;
		this.decltype = type;
		this.expr = expr;
	}

	public String getName() {
		return this.name;
	}

	public TType getDeclType() {
		return this.decltype;
	}

	public TCode getInner() {
		return this.expr;
	}

	@Override
	public TSkeleton getTemplate(TEnv env) {
		return env.getTemplate("let", "%2$s=%3$s");
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.decltype.strOut(env), this.name, this.expr.strOut(env));
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushLet(env, this);
	}

}

package blue.origami.transpiler.code;

import blue.origami.transpiler.FunctionContext;
import blue.origami.transpiler.FunctionContext.Variable;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.Ty;
import blue.origami.util.ODebug;
import blue.origami.util.StringCombinator;

public class LetCode extends Code1 {
	private Ty declType;
	private String name;
	private boolean isDuplicated = false;

	public LetCode(String name, Ty type, Code expr) {
		super(expr);
		this.name = name;
		this.declType = type;
	}

	public String getName() {
		return this.name;
	}

	public Ty getDeclType() {
		return this.declType;
	}

	public boolean isDuplicated() {
		return this.isDuplicated;
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
		if (this.isUntyped()) {
			this.inner = this.inner.asType(env, this.declType);
			if (this.declType.isUntyped()) {
				this.declType = this.inner.guessType();
			}
			this.setType(Ty.tVoid);
		}
		FunctionContext fcx = env.get(FunctionContext.class);
		assert (fcx != null);
		if (fcx.isDuplicatedName(this.name, this.declType)) {
			this.isDuplicated = true;
			ODebug.trace("duplicated local name %s", this.name);
		}
		Variable var = fcx.newVariable(this.name, this.declType);
		env.add(this.name, var);
		return this;
	}

	@Override
	public Template getTemplate(TEnv env) {
		return env.getTemplate("let", "%2$s=%3$s");
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.declType.strOut(env), this.name, this.getInner().strOut(env));
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

package blue.origami.transpiler.code;

import blue.origami.transpiler.SourceSection;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.Template;
import blue.origami.util.StringCombinator;

public class IfCode extends CodeN {

	public IfCode(Code condCode, Code thenCode, Code elseCode) {
		super(AutoType, condCode, thenCode, elseCode);
	}

	public Code condCode() {
		return this.args[0];
	}

	public Code thenCode() {
		return this.args[1];
	}

	public Code elseCode() {
		return this.args[2];
	}

	@Override
	public boolean hasReturn() {
		assert (this.args[1].hasReturn() == this.args[2].hasReturn());
		return this.args[1].hasReturn();
	}

	@Override
	public Code addReturn() {
		this.args[1] = this.args[1].addReturn();
		this.args[2] = this.args[2].addReturn();
		return this;
	}

	/* type dependency */

	@Override
	public Ty getType() {
		Ty t = this.args[1].getType();
		return (t.isUntyped()) ? this.args[2].getType() : t;
	}

	@Override
	public Code asType(TEnv env, Ty t) {
		this.args[1] = this.args[1].asType(env, t);
		this.args[2] = this.args[2].asType(env, t);
		return super.asType(env, t);
	}

	@Override
	public Template getTemplate(TEnv env) {
		return env.getTemplate("ifexpr", "(%1$s?%2$s:%3$)");
	}

	public boolean isStatementStyle() {
		return this.getType() == Ty.tVoid || this.hasReturn();
	}

	@Override
	public String strOut(TEnv env) {
		if (this.isStatementStyle()) {
			SourceSection p = env.getCurrentSourceSection();
			SourceSection sec = p.dup();
			env.setCurrentSourceSection(sec);
			sec.pushIf(env, this);
			env.setCurrentSourceSection(p);
			return sec.toString();
		}
		return super.strOut(env);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushIf(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("if(");
		StringCombinator.append(sb, this.args[0]);
		sb.append(") ");
		StringCombinator.append(sb, this.args[1]);
		sb.append(" then ");
		StringCombinator.append(sb, this.args[2]);
	}

}
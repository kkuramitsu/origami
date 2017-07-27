package blue.origami.transpiler.code;

import blue.origami.transpiler.SourceSection;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;

public class TIfCode extends MultiCode {

	public TIfCode(TCode condCode, TCode thenCode, TCode elseCode) {
		super(condCode, thenCode, elseCode);
	}

	public TCode condCode() {
		return this.args[0];
	}

	public TCode thenCode() {
		return this.args[1];
	}

	public TCode elseCode() {
		return this.args[2];
	}

	@Override
	public boolean hasReturn() {
		assert (this.args[1].hasReturn() == this.args[2].hasReturn());
		return this.args[1].hasReturn();
	}

	@Override
	public TCode addReturn() {
		this.args[1] = this.args[1].addReturn();
		this.args[2] = this.args[2].addReturn();
		return this;
	}

	/* type dependency */

	@Override
	public TType getType() {
		TType t = this.args[1].getType();
		return (t.isUntyped()) ? this.args[2].getType() : t;
	}

	@Override
	public TCode asType(TEnv env, TType t) {
		this.args[1] = this.args[1].asType(env, t);
		this.args[2] = this.args[2].asType(env, t);
		return super.asType(env, t);
	}

	@Override
	public Template getTemplate(TEnv env) {
		return env.getTemplate("ifexpr", "(%1$s?%2$s:%3$)");
	}

	public boolean isStatementStyle() {
		return this.getType() == TType.tVoid || this.hasReturn();
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

}
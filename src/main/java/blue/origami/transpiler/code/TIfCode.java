package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.TType;

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

	// @Override
	// public boolean hasReturnCode() {
	// if (this.args.length == 2) {
	// return this.args[1].hasReturnCode() && this.args[2].hasReturnCode();
	// }
	// return false;
	// }

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
		return env.getTemplate("ifexpr", "%1$s?%2$s:%3$");
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushIf(env, this);
	}

}
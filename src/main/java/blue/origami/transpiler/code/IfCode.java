package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.type.Ty;
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
		return (this.args[1].isError()) ? this.args[2].getType() : this.args[1].getType();
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
		this.args[0] = this.args[0].asType(env, Ty.tBool);
		this.args[1] = this.args[1].asType(env, ret);
		this.args[2] = this.args[2].asType(env, ret);
		return super.castType(env, ret);
	}

	public boolean isStatementStyle() {
		return this.getType().isVoid() || this.hasReturn();
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
		sb.append(" else ");
		StringCombinator.append(sb, this.args[2]);
	}

}
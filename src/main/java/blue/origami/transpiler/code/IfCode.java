package blue.origami.transpiler.code;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.type.Ty;

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
	public Code asType(Env env, Ty ret) {
		this.args[0] = this.args[0].asType(env, Ty.tBool);
		this.args[1] = this.args[1].asType(env, ret);
		this.args[2] = this.args[2].asType(env, ret);
		return super.castType(env, ret);
	}

	public boolean isStatementStyle() {
		return this.getType().isVoid() || this.hasReturn();
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushIf(this);
	}

	@Override
	public void dumpCode(SyntaxBuilder sb) {
		sb.Keyword("if ");
		sb.Expr(this.args[0]);
		sb.Keyword(" then ");
		sb.Expr(this.args[1]);
		sb.append(" else ");
		sb.Expr(this.args[2]);
	}

}

// public class WhileCode extends CodeN {
//
// public WhileCode(Code condCode, Code bodyCode) {
// super(AutoType, condCode, bodyCode);
// }
//
// public Code condCode() {
// return this.args[0];
// }
//
// public Code bodyCode() {
// return this.args[1];
// }
//
// @Override
// public Ty getType() {
// return Ty.tVoid;
// }
//
// @Override
// public Code asType(Env env, Ty ret) {
// this.args[0] = this.args[0].asType(env, Ty.tBool);
// this.args[1] = this.args[1].asType(env, ret);
// return super.castType(env, ret);
// }
//
// @Override
// public void emitCode(CodeSection sec) {
// sec.pushWhile(this);
// }
//
// @Override
// public void dumpCode(SyntaxBuilder sb) {
// sb.Keyword("if ");
// sb.Expr(this.args[0]);
// sb.Keyword(" then ");
// sb.Expr(this.args[1]);
// sb.append(" else ");
// sb.Expr(this.args[2]);
// }
//
// }
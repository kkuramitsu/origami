package blue.origami.transpiler.code;

import blue.origami.common.OStrings;
import blue.origami.common.SyntaxBuilder;

public class BinaryCode extends ExprCode implements CodeBuilder {

	public BinaryCode(String name, Code left, Code right) {
		super(name, CodeBuilder.groupfy(left), CodeBuilder.groupfy(right));
	}

	// @Override
	// protected void typeArgs(TEnv env, List<Template> l) {
	// super.typeArgs(env, l);
	// // Ty ty = this.args[0].getType();
	// // if (ty.hasVar()) {
	// // ty.acceptTy(bSUB, this.args[1].getType(), VarLogger.Update);
	// // ODebug.trace("binary typing %s %s", this.args[0].getType(),
	// // this.args[1].getType());
	// // return;
	// // }
	// // ty = this.args[1].getType();
	// // if (ty.hasVar()) {
	// // ODebug.trace("binary typing %s %s", this.args[0].getType(),
	// // this.args[1].getType());
	// // ty.acceptTy(bSUB, this.args[0].getType(), VarLogger.Update);
	// // }
	// }

	@Override
	public void strOut(StringBuilder sb) {
		OStrings.joins(sb, this.args, this.name);
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.TypeAnnotation(this.getType(), () -> {
			sh.Expr(this.args[0]);
			sh.s();
			sh.Operator(this.name);
			sh.s();
			sh.Expr(this.args[1]);
		});
	}

}
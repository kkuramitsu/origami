package blue.origami.transpiler.code;

import blue.origami.common.OStrings;
import blue.origami.common.SyntaxBuilder;

public class BinaryCode extends ExprCode implements CodeBuilder {

	public BinaryCode(String name, Code left, Code right) {
		super(name, CodeBuilder.groupfy(left), CodeBuilder.groupfy(right));
	}

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
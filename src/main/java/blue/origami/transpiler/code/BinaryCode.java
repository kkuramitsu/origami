package blue.origami.transpiler.code;

import java.util.List;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.Ty;
import blue.origami.util.ODebug;
import blue.origami.util.StringCombinator;

public class BinaryCode extends ExprCode implements CodeBuilder {

	public BinaryCode(String name, Code left, Code right) {
		super(name, CodeBuilder.groupfy(left), CodeBuilder.groupfy(right));
	}

	@Override
	protected void typeArgs(TEnv env, List<Template> l) {
		super.typeArgs(env, l);
		Ty ty = this.args[0].getType();
		if (ty.hasVar()) {
			ty.acceptTy(bSUB, this.args[1].getType(), bUPDATE);
			ODebug.trace("binary typing %s %s", this.args[0].getType(), this.args[1].getType());
			return;
		}
		ty = this.args[1].getType();
		if (ty.hasVar()) {
			ODebug.trace("binary typing %s %s", this.args[0].getType(), this.args[1].getType());
			ty.acceptTy(bSUB, this.args[0].getType(), bUPDATE);
		}
	}

	@Override
	public void strOut(StringBuilder sb) {
		StringCombinator.joins(sb, this.args, this.name);
	}

}
package blue.origami.transpiler.code;

import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.type.Ty;

public class ExistFieldCode extends Code1 {

	private String name;

	public ExistFieldCode(Code inner, String name) {
		super(inner);
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
		if (this.isUntyped()) {
			this.asTypeAt(env, 0, Ty.tData());
			this.setType(Ty.tBool);
		}
		return this.castType(env, ret);
	}

	@Override
	public void emitCode(TEnv env, CodeSection sec) {
		sec.pushExistField(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dumpCode(SyntaxHighlight sh) {
		sh.Name(this.name);
		sh.Keyword(" in ");
		sh.Expr(this.getInner());
	}

}

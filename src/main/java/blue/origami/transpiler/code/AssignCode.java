package blue.origami.transpiler.code;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.type.Ty;
import origami.nez2.Token;

public class AssignCode extends Code1 {
	public String name;
	public int index = -1;

	public AssignCode(Token ns, Code expr) {
		super(expr);
		this.atname = ns;
		this.name = ns.getSymbol();
	}

	public AssignCode(String name, Code expr) {
		this(NameHint.getName(name), expr);
	}

	public String getName() {
		return this.index == -1 ? this.name : this.name + this.index;
	}

	@Override
	public Code asType(Env env, Ty ret) {
		return env.getLanguage().typeAssign(env, this, ret);
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushAssign(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.sexpr(sb, "assign " + this.getName(), this.getInner());
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Name(this.name);
		sh.Operator(" = ");
		sh.Expr(this.getInner());
	}
}
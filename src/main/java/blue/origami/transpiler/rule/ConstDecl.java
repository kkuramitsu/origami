package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.LetCode;
import blue.origami.transpiler.type.Ty;

public class ConstDecl extends SyntaxRule implements ParseRule {

	private boolean isPublic;

	public ConstDecl() {
		this(false);
	}

	public ConstDecl(boolean isPublic) {
		this.isPublic = isPublic;
	}

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		String name = t.getStringAt(_name, "");
		Code right = env.parseCode(env, t.get(_expr));
		Ty type = t.has(_type) ? env.parseType(env, t.get(_type, null), null) : null;
		LetCode let = new LetCode(name, type, right);
		return let.defineAsGlobal(env, this.isPublic);
	}

}
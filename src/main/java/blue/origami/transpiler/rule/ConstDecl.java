package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.FunctionContext;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DeclCode;
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
		//
		FunctionContext fcx = env.get(FunctionContext.class);
		if (type == null) {
			type = Ty.tUntyped();
			right = right.bind(type).asType(env, type);
			type = right.getType();
		} else {
			right = right.bind(type).asType(env, type);
		}
		//
		Transpiler tp = env.getTranspiler();
		Template defined = tp.defineConst(this.isPublic, name, type, right);
		env.add(name, defined);
		return new DeclCode();
	}

}
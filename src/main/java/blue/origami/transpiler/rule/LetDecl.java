package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFunctionContext;
import blue.origami.transpiler.TFunctionContext.TVariable;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TDeclCode;
import blue.origami.transpiler.code.TLetCode;

public class LetDecl extends SyntaxRule implements TTypeRule {

	private boolean isPublic;

	public LetDecl() {
		this(false);
	}

	public LetDecl(boolean isPublic) {
		this.isPublic = isPublic;
	}

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		String name = t.getStringAt(_name, "");
		TCode right = env.parseCode(env, t.get(_expr));
		TType type = t.has(_type) //
				? env.parseType(env, t.get(_type, null), null)//
				: right.asType(env, TType.tUntyped).guessType();
		// FIXME: type = env.parseTypeArity(env, type, t);
		right = right.asType(env, type);

		TFunctionContext fcx = env.get(TFunctionContext.class);
		if (fcx == null) {
			Transpiler tp = env.getTranspiler();
			Template defined = tp.defineConst(this.isPublic, name, type, right);
			env.add(name, defined);
			return new TDeclCode();
		} else {
			TVariable var = fcx.newVariable(name, type);
			env.add(name, var);
			return new TLetCode(var.getName(), type, right);
		}
	}

}

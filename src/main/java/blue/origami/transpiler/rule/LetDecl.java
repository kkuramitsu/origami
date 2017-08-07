package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFunctionContext;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TDeclCode;
import blue.origami.transpiler.code.TLetCode;

public class LetDecl extends SyntaxRule implements ParseRule {

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

		TFunctionContext fcx = env.get(TFunctionContext.class);
		if (fcx == null) { // TopLevel
			Ty type = t.has(_type) //
					? env.parseType(env, t.get(_type, null), null)//
					: right.asType(env, Ty.tUntyped).guessType();
			// FIXME: type = env.parseTypeArity(env, type, t);
			right = right.asType(env, type);
			//
			Transpiler tp = env.getTranspiler();
			Template defined = tp.defineConst(this.isPublic, name, type, right);
			env.add(name, defined);
			return new TDeclCode();
		} else {
			Ty type = t.has(_type) ? env.parseType(env, t.get(_type, null), null) : Ty.tUntyped;
			return new TLetCode(name, type, right);
		}
	}

}

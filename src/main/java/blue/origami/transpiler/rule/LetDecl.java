package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.LetCode;
import blue.origami.transpiler.type.Ty;

public class LetDecl extends SyntaxRule implements ParseRule {

	@Override
	public Code apply(Env env, AST t) {
		Code right = env.parseCode(env, t.get(_expr));
		Ty type = t.has(_type) ? env.parseType(env, t.get(_type), null) : null;
		return new LetCode(t.get(_name), type, right);
	}

}

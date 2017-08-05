package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.rule.OSymbols;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TIfCode;
import blue.origami.transpiler.code.TMultiCode;

public class IfExpr implements ParseRule, OSymbols {

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		TCode condCode = env.parseCode(env, t.get(_cond)).asType(env, TType.tBool);
		TCode thenCode = env.parseCode(env, t.get(_then));
		TCode elseCode = t.has(_else) ? env.parseCode(env, t.get(_else)) : new TMultiCode(false);
		return new TIfCode(condCode, thenCode, elseCode);
	}

}

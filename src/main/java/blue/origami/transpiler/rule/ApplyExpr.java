package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.rule.OSymbols;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.TCode;

public class ApplyExpr implements ParseRule, OSymbols {

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		TCode[] params = env.parseParams(env, t, _param);
		TCode recv = env.parseCode(env, t.get(_recv));
		return recv.applyCode(env, params);
	}
}

// public TTypeRule NewArrayExpr = new TypeRule() {
// @Override
// public TCode typeRule(TEnv env, Tree<?> t) {
// TType type = OrigamiExpressionRules.this.parseType(env, t.get(_type),
// null);
// TCode[] expr = null;
// if (t.has(_expr)) {
// Tree<?> exprs = t.get(_expr);
// expr = new TCode[exprs.size()];
// for (int i = 0; i < expr.length; i++) {
// expr[i] = OrigamiExpressionRules.this.typeExpr(env, exprs.get(i));
// }
// }
//
// return new ArrayCode(type, expr);
// }
// };

package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.rule.OSymbols;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TDataRangeCode;
import blue.origami.transpiler.code.TIntCode;

public class RangeExpr implements ParseRule, OSymbols {
	boolean inclusive;

	public RangeExpr() {
		this.inclusive = true;
	}

	public RangeExpr(boolean inclusive) {
		this.inclusive = inclusive;
	}

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		TCode left = env.parseCode(env, t.get(_left));
		TCode right = env.parseCode(env, t.get(_right));
		if (!this.inclusive) {
			right = right.applyMethodCode(env, "+", new TIntCode(1));
		}
		return new TDataRangeCode(left, right);
	}

}

package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataRangeCode;
import blue.origami.transpiler.code.IntCode;

public class RangeExpr implements ParseRule, Symbols {
	boolean inclusive;

	public RangeExpr() {
		this.inclusive = true;
	}

	public RangeExpr(boolean inclusive) {
		this.inclusive = inclusive;
	}

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		Code left = env.parseCode(env, t.get(_left));
		Code right = env.parseCode(env, t.get(_right));
		if (!this.inclusive) {
			right = right.applyMethodCode(env, "+", new IntCode(1));
		}
		return new DataRangeCode(left, right);
	}

}

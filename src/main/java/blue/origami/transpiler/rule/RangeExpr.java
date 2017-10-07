package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.BinaryCode;
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
	public Code apply(TEnv env, AST t) {
		Code left = env.parseCode(env, t.get(_left));
		Code right = env.parseCode(env, t.get(_right));
		if (!this.inclusive) {
			right = new BinaryCode("-", right, new IntCode(1));
		}
		return new DataRangeCode(left, right);
	}

}

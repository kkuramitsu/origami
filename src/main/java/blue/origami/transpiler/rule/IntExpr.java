package blue.origami.transpiler.rule;

import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TIntCode;

public class IntExpr extends NumberExpr implements TTypeRule {

	@Override
	protected TCode newCode(Number value) {
		return new TIntCode(value.intValue());
	}

}

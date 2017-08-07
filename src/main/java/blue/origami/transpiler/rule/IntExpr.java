package blue.origami.transpiler.rule;

import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.IntCode;

public class IntExpr extends NumberExpr implements ParseRule {

	@Override
	protected Code newCode(Number value) {
		return new IntCode(value.intValue());
	}

}

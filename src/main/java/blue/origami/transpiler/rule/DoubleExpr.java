package blue.origami.transpiler.rule;

import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DoubleCode;

public class DoubleExpr extends NumberExpr implements ParseRule {

	@Override
	protected Code newCode(Number value) {
		return new DoubleCode(value.doubleValue());
	}

}

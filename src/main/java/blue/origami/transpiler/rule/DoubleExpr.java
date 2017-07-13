package blue.origami.transpiler.rule;

import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TDoubleCode;

public class DoubleExpr extends NumberExpr implements TTypeRule {

	@Override
	protected TCode newCode(Number value) {
		return new TDoubleCode(value.doubleValue());
	}

}
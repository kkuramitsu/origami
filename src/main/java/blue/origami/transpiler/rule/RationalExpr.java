package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DoubleCode;
import blue.origami.transpiler.code.ErrorCode;
import origami.nez2.ParseTree;

public class RationalExpr implements ParseRule {

	@Override
	public Code apply(Env env, ParseTree t) {
		String[] text = t.asString().split("/");
		double a = Double.parseDouble(text[0]);
		double b = Double.parseDouble(text[1]);
		try {
			return new DoubleCode(a / b);
		} catch (Exception e) {
			throw new ErrorCode(env.s(t), TFmt.wrong_number_format_YY1_by_YY2, t.asString(), e);
		}
	}

}
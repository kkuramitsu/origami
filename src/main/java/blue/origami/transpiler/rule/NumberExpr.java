package blue.origami.transpiler.rule;

import java.math.BigDecimal;
import java.math.BigInteger;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.TLog;
import blue.origami.transpiler.code.TCode;
import blue.origami.util.ODebug;

public abstract class NumberExpr extends LoggerRule implements ParseRule {
	public final Class<?> baseType;

	public NumberExpr() {
		this(double.class);
	}

	NumberExpr(Class<?> baseType) {
		this.baseType = baseType;
	}

	protected abstract TCode newCode(Number value);

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		TLog log = null;
		String text = t.getString().replace("_", "");
		int radix = 10;
		if (text.endsWith("L") || text.endsWith("l")) {
			text = text.substring(0, text.length() - 1);
		}
		if (text.startsWith("0b") || text.startsWith("0B")) {
			text = text.substring(2);
			radix = 2;
		} else if (text.startsWith("0x") || text.startsWith("0X")) {
			text = text.substring(2);
			radix = 16;
		} else if (text.startsWith("0")) {
			radix = 8;
		}
		Class<?> base = this.baseType;
		Number value = null;
		if (base == int.class) {
			try {
				value = Integer.parseInt(text, radix);
			} catch (NumberFormatException e) {
				ODebug.trace("radix=%d", radix);
				log = this.reportWarning(log, t, TFmt.wrong_number_format_YY0_by_YY1, text, e);
				value = 0;
			}
		} else if (base == double.class) {
			try {
				value = Double.parseDouble(text);
			} catch (NumberFormatException e) {
				log = this.reportWarning(log, t, TFmt.wrong_number_format_YY0_by_YY1, text, e);
				value = 0.0;
			}
		} else if (base == long.class) {
			try {
				value = Long.parseLong(text, radix);
			} catch (NumberFormatException e) {
				log = this.reportWarning(log, t, TFmt.wrong_number_format_YY0_by_YY1, text, e);
				value = 0L;
			}
		} else if (base == float.class) {
			try {
				value = Float.parseFloat(text);
			} catch (NumberFormatException e) {
				log = this.reportWarning(log, t, TFmt.wrong_number_format_YY0_by_YY1, text, e);
				value = 0.0f;
			}
		} else if (base == BigInteger.class) {
			try {
				value = new BigInteger(text, radix);
			} catch (NumberFormatException e2) {
				log = this.reportWarning(log, t, TFmt.wrong_number_format_YY0_by_YY1, text, e2);
				value = BigInteger.ZERO;
			}
		} else {
			try {
				value = new BigDecimal(text);
			} catch (NumberFormatException e2) {
				log = this.reportWarning(log, t, TFmt.wrong_number_format_YY0_by_YY1, text, e2);
				value = BigDecimal.ZERO;
			}
		}
		return this.log(log, this.newCode(value));
	}

}

package blue.nez.peg.expression;

import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;
import blue.nez.peg.NezFunc;

public class PScan extends PFunction<String> {
	public final long mask;
	public final int shift;

	public PScan(String pattern, Expression e) {
		super(NezFunc.scanf, pattern, e);
		long bits = 0;
		int shift = 0;
		if (pattern != null) {
			bits = Long.parseUnsignedLong(pattern, 2);
			long m = bits;
			while ((m & 1L) == 0) {
				m >>= 1;
				shift++;
			}
			// factory.verbose("@@ mask=%s, shift=%d,%d", mask, bits,
			// shift);
		}
		this.mask = bits;
		this.shift = shift;
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitScan(this, a);
	}
}
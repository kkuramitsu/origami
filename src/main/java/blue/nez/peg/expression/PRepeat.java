package blue.nez.peg.expression;

import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;
import blue.nez.peg.NezFunc;

public class PRepeat extends PFunction<Object> {
	public PRepeat(Expression e, Object ref) {
		super(NezFunc.repeat, null, e, ref);
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitRepeat(this, a);
	}
}
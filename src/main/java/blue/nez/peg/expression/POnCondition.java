package blue.nez.peg.expression;

import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;
import blue.nez.peg.NezFunc;

public class POnCondition extends PFunction<String> {
	public final boolean isPositive() {
		return !this.param.startsWith("!");
	}

	public final String flagName() {
		return this.param.startsWith("!") ? this.param.substring(1) : this.param;
	}

	public POnCondition(String c, Expression e, Object ref) {
		super(NezFunc.on, c, e, ref);
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitOn(this, a);
	}
}
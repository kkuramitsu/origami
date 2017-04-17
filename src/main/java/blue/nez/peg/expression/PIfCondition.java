package blue.nez.peg.expression;

import blue.nez.peg.ExpressionVisitor;
import blue.nez.peg.NezFunc;

public class PIfCondition extends PFunction<String> {
	public final boolean isPositive() {
		return !this.param.startsWith("!");
	}

	public final String flagName() {
		return this.param.startsWith("!") ? this.param.substring(1) : this.param;
	}

	public PIfCondition(String c, Object ref) {
		super(NezFunc._if, c, defaultEmpty, ref);
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitIf(this, a);
	}
}
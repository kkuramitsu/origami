package blue.nez.peg.expression;

import blue.nez.peg.Expression;

abstract class PTerm extends Expression {
	protected PTerm(Object ref) {
		super(ref);
	}

	@Override
	public final int size() {
		return 0;
	}

	@Override
	public final Expression get(int index) {
		return null;
	}
}
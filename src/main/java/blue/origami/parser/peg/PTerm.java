package blue.origami.parser.peg;

abstract class PTerm extends Expression {
	protected PTerm() {
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
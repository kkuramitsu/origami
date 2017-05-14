package blue.nez.parser.pasm;

public final class Peof extends PAsmInst {

	public Peof(PAsmInst next) {
		super(next);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return (neof(px)) ? raiseFail(px) : this.next;
	}
}
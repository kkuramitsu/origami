package blue.nez.parser.pasm;

public final class Nany extends PAsmInst {

	public Nany(PAsmInst next) {
		super(next);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return (neof(px)) ? raiseFail(px) : this.next;
	}
}
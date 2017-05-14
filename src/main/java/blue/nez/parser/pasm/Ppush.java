package blue.nez.parser.pasm;

public final class Ppush extends PAsmInst {
	public Ppush(PAsmInst next) {
		super(next);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		pushPos(px);
		return this.next;
	}

}
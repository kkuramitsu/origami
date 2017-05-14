package blue.nez.parser.pasm;

public final class Spop extends PAsmInst {
	public Spop(PAsmInst next) {
		super(next);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		popState(px);
		return this.next;
	}
}
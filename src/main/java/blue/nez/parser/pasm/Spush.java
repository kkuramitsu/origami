package blue.nez.parser.pasm;

public final class Spush extends PAsmInst {
	public Spush(PAsmInst next) {
		super(next);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		pushState(px);
		return this.next;
	}

}
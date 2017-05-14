package blue.nez.parser.pasm;

public final class Isucc extends PAsmInst {
	public Isucc(PAsmInst next) {
		super(next);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		popFail(px);
		return this.next;
	}
}
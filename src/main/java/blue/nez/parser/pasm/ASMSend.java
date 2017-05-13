package blue.nez.parser.pasm;

public final class ASMSend extends PAsmInst {
	public ASMSend(PAsmInst next) {
		super(next);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		popState(px);
		return this.next;
	}
}
package blue.nez.parser.pasm;

public final class ASMsucc extends PAsmInst {
	public ASMsucc(PAsmInst next) {
		super(next);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		popFail(px);
		return this.next;
	}
}
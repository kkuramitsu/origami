package blue.nez.parser.pasm;

public final class ASMSbegin extends PAsmInst {
	public ASMSbegin(PAsmInst next) {
		super(next);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		pushState(px);
		return this.next;
	}

}
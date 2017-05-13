package blue.nez.parser.pasm;

public final class ASMback extends PAsmInst {
	public ASMback(PAsmInst next) {
		super(next);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		popPos(px);
		return this.next;
	}

}
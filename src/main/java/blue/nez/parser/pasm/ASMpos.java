package blue.nez.parser.pasm;

public final class ASMpos extends PAsmInst {
	public ASMpos(PAsmInst next) {
		super(next);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		popPos(px);
		return this.next;
	}

}
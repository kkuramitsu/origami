package blue.nez.parser.pasm;

public final class ASMTpush extends PAsmInst {
	public ASMTpush(PAsmInst next) {
		super(next);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		pushTree(px);
		return this.next;
	}

}
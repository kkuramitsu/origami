package blue.nez.parser.pasm;

public final class ASMTpop extends PAsmInst {

	public ASMTpop(PAsmInst next) {
		super(next);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		popTree(px);
		return this.next;
	}

}
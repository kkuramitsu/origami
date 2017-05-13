package blue.nez.parser.pasm;

public final class ASMalt extends PAsmInst {
	public final PAsmInst jump; // jump if failed

	public ASMalt(PAsmInst next, PAsmInst failjump) {
		super(next);
		this.jump = PegAsm.joinPoint(failjump);
	}

	@Override
	public PAsmInst branch() {
		return this.jump;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		pushFail(px, this.jump);
		return this.next;
	}

}
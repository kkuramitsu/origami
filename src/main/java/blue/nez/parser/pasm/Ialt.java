package blue.nez.parser.pasm;

public final class Ialt extends PAsmInst {
	public final PAsmInst jump; // jump if failed

	public Ialt(PAsmInst next, PAsmInst failjump) {
		super(next);
		this.jump = failjump;
	}

	@Override
	public PAsmInst[] branch() {
		return new PAsmInst[] { this.jump };
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		pushFail(px, this.jump);
		return this.next;
	}

}
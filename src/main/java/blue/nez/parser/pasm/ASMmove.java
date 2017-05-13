package blue.nez.parser.pasm;

public final class ASMmove extends PAsmInst {
	public final int shift;

	public ASMmove(int shift, PAsmInst next) {
		super(next);
		this.shift = shift;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		move(px, this.shift);
		return this.next;
	}

}
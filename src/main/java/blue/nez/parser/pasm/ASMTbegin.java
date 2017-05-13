package blue.nez.parser.pasm;

public final class ASMTbegin extends PAsmInst {
	public final int shift;

	public ASMTbegin(int shift, PAsmInst next) {
		super(next);
		this.shift = shift;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		beginTree(px, this.shift);
		return this.next;
	}
}
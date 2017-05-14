package blue.nez.parser.pasm;

public final class Tbegin extends PAsmInst {
	public final int shift;

	public Tbegin(int shift, PAsmInst next) {
		super(next);
		this.shift = shift;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		beginTree(px, this.shift);
		return this.next;
	}
}
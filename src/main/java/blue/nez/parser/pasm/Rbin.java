package blue.nez.parser.pasm;

public class Rbin extends PAsmInst {
	public final int[] bits;

	public Rbin(int[] bits, PAsmInst next) {
		super(next);
		this.bits = bits;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		while (bitis(this.bits, getbyte(px)) && neof(px)) {
			move(px, 1);
		}
		return this.next;
	}
}
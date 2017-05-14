package blue.nez.parser.pasm;

public class Oset extends PAsmInst {
	public final int[] bits;

	public Oset(int[] bits, PAsmInst next) {
		super(next);
		this.bits = bits;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		if (bitis(bits, getbyte(px))) {
			move(px, 1);
		}
		return this.next;
	}

}
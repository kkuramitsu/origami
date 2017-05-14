package blue.nez.parser.pasm;

public class Pbis extends PAsmInst {
	public final int[] bits;

	public Pbis(int[] bits, PAsmInst next) {
		super(next);
		this.bits = bits;
		assert (bitis(this.bits, 0));
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		int c = getbyte(px);
		if (c == 0) {
			return neof(px) ? this.next : raiseFail(px);
		}
		return bitis(this.bits, c) ? this.next : raiseFail(px);
	}

}
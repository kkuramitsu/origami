package blue.nez.parser.pasm;

public class Pis extends PAsmInst {
	public final int[] bits;

	public Pis(int[] bits, PAsmInst next) {
		super(next);
		this.bits = bits;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return bitis(this.bits, getbyte(px)) ? this.next : raiseFail(px);
	}

}
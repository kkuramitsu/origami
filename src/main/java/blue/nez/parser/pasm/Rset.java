package blue.nez.parser.pasm;

public class Rset extends PAsmInst {
	public final int[] bits;

	public Rset(int[] bits, PAsmInst next) {
		super(next);
		this.bits = bits;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		while (bitis(this.bits, getbyte(px))) {
			move(px, 1);
		}
		return this.next;
	}

}
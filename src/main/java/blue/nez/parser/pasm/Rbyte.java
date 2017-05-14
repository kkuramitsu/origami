package blue.nez.parser.pasm;

public class Rbyte extends PAsmInst {
	public final int byteChar;

	public Rbyte(int byteChar, PAsmInst next) {
		super(next);
		this.byteChar = byteChar;
		assert (byteChar != 0);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		while (getbyte(px) == this.byteChar) {
			move(px, 1);
		}
		return this.next;
	}
}
package blue.nez.parser.pasm;

public class Obyte extends PAsmInst {
	public final int byteChar;

	public Obyte(int byteChar, PAsmInst next) {
		super(next);
		this.byteChar = byteChar;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		if (getbyte(px) == this.byteChar) {
			move(px, 1);
		}
		return this.next;
	}
}
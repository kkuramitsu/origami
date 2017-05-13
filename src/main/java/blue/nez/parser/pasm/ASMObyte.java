package blue.nez.parser.pasm;

public class ASMObyte extends PAsmInst {
	public final int byteChar;

	public ASMObyte(int byteChar, PAsmInst next) {
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
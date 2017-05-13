package blue.nez.parser.pasm;

public class ASMNbyte extends PAsmInst {
	public final int byteChar;

	public ASMNbyte(int byteChar, PAsmInst next) {
		super(next);
		this.byteChar = byteChar;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		if (getbyte(px) != this.byteChar) {
			return this.next;
		}
		return raiseFail(px);
	}

}
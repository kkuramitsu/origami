package blue.nez.parser.pasm;

public class Nbyte extends PAsmInst {
	public final int byteChar;

	public Nbyte(int byteChar, PAsmInst next) {
		super(next);
		this.byteChar = byteChar;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return (getbyte(px) != this.byteChar) ? this.next : raiseFail(px);
	}

}
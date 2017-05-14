package blue.nez.parser.pasm;

public class Pbyte extends PAsmInst {
	public final int byteChar;

	public Pbyte(int byteChar, PAsmInst next) {
		super(next);
		this.byteChar = byteChar;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return (nextbyte(px) == this.byteChar) ? this.next : raiseFail(px);
	}
}
package blue.nez.parser.pasm;

public class Idispatch extends PAsmInst {
	public final byte[] jumpIndex;
	public final PAsmInst[] jumpTable;

	public Idispatch(byte[] jumpIndex, PAsmInst[] jumpTable) {
		super(null);
		this.jumpIndex = jumpIndex;
		this.jumpTable = jumpTable;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return this.jumpTable[this.jumpIndex[getbyte(px)] & 0xff];
	}

	@Override
	public PAsmInst[] branch() {
		return this.jumpTable;
	}

}
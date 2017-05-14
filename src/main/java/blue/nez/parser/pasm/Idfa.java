package blue.nez.parser.pasm;

public final class Idfa extends PAsmInst {
	public final byte[] jumpIndex;
	public final PAsmInst[] jumpTable;

	public Idfa(byte[] jumpIndex, PAsmInst[] jumpTable) {
		super(null);
		this.jumpIndex = jumpIndex;
		this.jumpTable = jumpTable;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return this.jumpTable[this.jumpIndex[nextbyte(px)] & 0xff];
	}

	@Override
	public PAsmInst[] branch() {
		return this.jumpTable;
	}

}
package blue.nez.parser.pasm;

public final class ASMOstr extends PAsmInst {
	public final byte[] utf8;

	public ASMOstr(byte[] byteSeq, PAsmInst next) {
		super(next);
		this.utf8 = byteSeq;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		matchBytes(px, this.utf8);
		return this.next;
	}

}
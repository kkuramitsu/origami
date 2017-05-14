package blue.nez.parser.pasm;

public final class Nstr extends PAsmInst {
	public final byte[] utf8;

	public Nstr(byte[] byteSeq, PAsmInst next) {
		super(next);
		this.utf8 = byteSeq;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return matchBytes(px, this.utf8) ? raiseFail(px) : this.next;
	}

}
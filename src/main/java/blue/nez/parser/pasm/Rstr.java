package blue.nez.parser.pasm;

public final class Rstr extends PAsmInst {
	public final byte[] utf8;

	public Rstr(byte[] byteSeq, PAsmInst next) {
		super(next);
		this.utf8 = byteSeq;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		while (matchBytes(px, this.utf8)) {
		}
		return this.next;
	}

}
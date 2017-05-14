package blue.nez.parser.pasm;

public final class Iupdate extends PAsmInst {
	public Iupdate() {
		super(null);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return updateFail(px, this.next);
	}
}
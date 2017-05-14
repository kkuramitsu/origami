package blue.nez.parser.pasm;

public final class Iexit extends PAsmInst {
	public final boolean status;

	public Iexit(boolean status) {
		super(null);
		this.status = status;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		throw new PAsmTerminationException(this.status);
	}
}
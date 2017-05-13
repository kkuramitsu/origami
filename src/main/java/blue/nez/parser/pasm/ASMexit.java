package blue.nez.parser.pasm;

public final class ASMexit extends PAsmInst {
	public final boolean status;

	public ASMexit(boolean status) {
		super(null);
		this.status = status;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		throw new PAsmTerminationException(this.status);
	}
}
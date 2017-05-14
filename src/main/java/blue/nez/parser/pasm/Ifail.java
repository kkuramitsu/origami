package blue.nez.parser.pasm;

public final class Ifail extends PAsmInst {
	public Ifail() {
		super(null);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return raiseFail(px);
	}

}
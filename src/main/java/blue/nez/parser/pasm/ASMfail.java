package blue.nez.parser.pasm;

public final class ASMfail extends PAsmInst {
	public ASMfail() {
		super(null);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return raiseFail(px);
	}

}
package blue.nez.parser.pasm;

public final class Iret extends PAsmInst {
	public Iret() {
		super(null);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return popRet(px);
	}

}
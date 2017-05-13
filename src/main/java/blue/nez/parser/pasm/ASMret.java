package blue.nez.parser.pasm;

public final class ASMret extends PAsmInst {
	public ASMret() {
		super(null);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return popRet(px);
	}

}
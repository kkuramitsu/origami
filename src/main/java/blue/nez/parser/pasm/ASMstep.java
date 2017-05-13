package blue.nez.parser.pasm;

public final class ASMstep extends PAsmInst {
	public ASMstep() {
		super(null);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return updateFail(px, this.next);
	}
}
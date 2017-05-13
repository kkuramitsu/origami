package blue.nez.parser.pasm;

public final class ASMguard extends PAsmInst {
	public ASMguard() {
		super(null);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return updateFail(px, this.next);
	}
}
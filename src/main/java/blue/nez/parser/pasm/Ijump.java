package blue.nez.parser.pasm;

public final class Ijump extends PAsmInst {
	public PAsmInst jump = null;

	public Ijump(PAsmInst jump) {
		super(null);
		this.jump = jump;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return this.jump;
	}

}
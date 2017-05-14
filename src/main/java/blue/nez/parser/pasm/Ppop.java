package blue.nez.parser.pasm;

public final class Ppop extends PAsmInst {
	public Ppop(PAsmInst next) {
		super(next);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		popPos(px);
		return this.next;
	}

}
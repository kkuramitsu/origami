package blue.nez.parser.pasm;

public final class ASMany extends PAsmInst {
	public ASMany(PAsmInst next) {
		super(next);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		if (neof(px)) {
			move(px, 1);
			return this.next;
		}
		return raiseFail(px);
	}
}
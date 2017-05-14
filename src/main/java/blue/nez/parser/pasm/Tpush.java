package blue.nez.parser.pasm;

public final class Tpush extends PAsmInst {
	public Tpush(PAsmInst next) {
		super(next);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		pushTree(px);
		return this.next;
	}

}
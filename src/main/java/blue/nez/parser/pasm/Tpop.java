package blue.nez.parser.pasm;

public final class Tpop extends PAsmInst {

	public Tpop(PAsmInst next) {
		super(next);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		popTree(px);
		return this.next;
	}

}
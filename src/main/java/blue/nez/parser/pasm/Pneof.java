package blue.nez.parser.pasm;

public class Pneof extends PAsmInst {

	public Pneof(PAsmInst next) {
		super(next);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return neof(px) ? this.next : raiseFail(px);
	}
}
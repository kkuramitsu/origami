package blue.nez.parser.pasm;

public class Neof extends PAsmInst {

	public Neof(PAsmInst next) {
		super(next);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return neof(px) ? this.next : raiseFail(px);
	}
}
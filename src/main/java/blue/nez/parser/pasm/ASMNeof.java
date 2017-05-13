package blue.nez.parser.pasm;

public class ASMNeof extends PAsmInst {

	public ASMNeof(PAsmInst next) {
		super(next);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return neof(px) ? this.next : raiseFail(px);
	}
}
package blue.nez.parser.pasm;

public final class Iguard extends PAsmInst {
	public Iguard() {
		super(null);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return updateFail(px, this.next);
	}
}
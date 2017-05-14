package blue.nez.parser.pasm;

public class Pset extends PAsmInst {
	public final boolean[] bools;

	public Pset(boolean[] bools, PAsmInst next) {
		super(next);
		this.bools = bools;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return (this.bools[nextbyte(px)]) ? this.next : raiseFail(px);
	}

}
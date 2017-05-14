package blue.nez.parser.pasm;

public class Nset extends PAsmInst {
	public final boolean[] bools;

	public Nset(boolean[] bools, PAsmInst next) {
		super(next);
		this.bools = bools;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return this.bools[getbyte(px)] ? raiseFail(px) : this.next;
	}

}
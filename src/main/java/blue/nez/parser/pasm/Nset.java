package blue.nez.parser.pasm;

public class Nset extends PAsmInst {
	public final boolean[] bools;

	public Nset(boolean[] bools, PAsmInst next) {
		super(next);
		this.bools = bools;
		this.bools[0] = true;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		int byteChar = getbyte(px);
		if (!this.bools[byteChar]) {
			return this.next;
		}
		return raiseFail(px);
	}

}
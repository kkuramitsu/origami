package blue.nez.parser.pasm;

public class Oset extends PAsmInst {
	public final boolean[] bools;

	public Oset(boolean[] bools, PAsmInst next) {
		super(next);
		this.bools = bools;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		if (this.bools[getbyte(px)]) {
			move(px, 1);
		}
		return this.next;
	}

}
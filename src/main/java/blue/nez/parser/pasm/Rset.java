package blue.nez.parser.pasm;

public class Rset extends PAsmInst {
	public final boolean[] bools;

	public Rset(boolean[] bools, PAsmInst next) {
		super(next);
		this.bools = bools;
		assert (bools[0] == false);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		while (this.bools[getbyte(px)]) {
			move(px, 1);
		}
		return this.next;
	}

}
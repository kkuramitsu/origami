package blue.nez.parser.pasm;

import blue.nez.parser.pasm.PAsmAPI.PAsmContext;

public class Rbin extends PAsmInst {
	public final boolean[] bools;

	public Rbin(boolean[] bools, PAsmInst next) {
		super(next);
		this.bools = bools;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		while (this.bools[getbyte(px)] && neof(px)) {
			move(px, 1);
		}
		return this.next;
	}
}
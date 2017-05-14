package blue.nez.parser.pasm;

import blue.nez.parser.pasm.PAsmAPI.PAsmContext;

public class Obin extends PAsmInst {
	public final boolean[] bools;

	public Obin(boolean[] bools, PAsmInst next) {
		super(next);
		this.bools = bools;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		int c = getbyte(px);
		if (this.bools[c] && (c != 0 || neof(px))) {
			move(px, 1);
		}
		return this.next;
	}

}
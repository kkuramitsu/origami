package blue.nez.parser.pasm;

import blue.nez.parser.pasm.PAsmAPI.PAsmContext;

public class Nbin extends PAsmInst {
	public final boolean[] bools;

	public Nbin(boolean[] bools, PAsmInst next) {
		super(next);
		this.bools = bools;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		int c = getbyte(px);
		if (this.bools[c]) {
			return raiseFail(px);
		}
		if (c == 0 && neof(px)) { // bools[0] == false
			return raiseFail(px);
		}
		return this.next;
	}

}
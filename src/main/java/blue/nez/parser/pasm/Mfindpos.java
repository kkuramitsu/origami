package blue.nez.parser.pasm;

import blue.nez.parser.ParserGrammar.MemoPoint;

public final class Mfindpos extends PAsmInst {
	public final int memoPoint;
	// public final PAsmInst ret;

	public Mfindpos(MemoPoint m, PAsmInst next, PAsmInst ret) {
		super(next);
		this.memoPoint = m.id;
		// this.ret = ret;
		assert (ret instanceof Iret);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		switch (lookupMemo1(px, this.memoPoint)) {
		case PAsmAPI.NotFound:
			return this.next;
		case PAsmAPI.SuccFound:
			// return this.ret;
			return popRet(px);
		default:
			return raiseFail(px);
		}
	}
}
package blue.nez.parser.pasm;

import blue.nez.parser.ParserGrammar.MemoPoint;

public final class Mfindtree extends PAsmInst {
	public final int memoPoint;
	// public final PAsmInst jump;

	public Mfindtree(MemoPoint m, PAsmInst unfound, PAsmInst ret) {
		super(unfound);
		this.memoPoint = m.id;
		assert (ret instanceof Iret);
		// this.jump = ret;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		switch (lookupMemo3(px, this.memoPoint)) {
		case PAsmAPI.NotFound:
			return this.next;
		case PAsmAPI.SuccFound:
			return popRet(px);
		// return this.jump;
		default:
			return raiseFail(px);
		}
	}

}
package blue.nez.parser.pasm;

import blue.nez.parser.ParserGrammar.MemoPoint;

public final class ASMMlookupTree extends PAsmInst {
	public final int memoPoint;
	public final PAsmInst jump;

	public ASMMlookupTree(MemoPoint m, PAsmInst found, PAsmInst unfound) {
		super(found);
		this.memoPoint = m.id;
		this.jump = unfound;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		switch (lookupMemo3(px, this.memoPoint)) {
		case PAsmAPI.NotFound:
			return this.next;
		case PAsmAPI.SuccFound:
			return this.jump;
		default:
			return raiseFail(px);
		}
	}

}
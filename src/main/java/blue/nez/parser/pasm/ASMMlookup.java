package blue.nez.parser.pasm;

import blue.nez.parser.ParserGrammar.MemoPoint;

public final class ASMMlookup extends PAsmInst {
	public final int memoPoint;
	public final PAsmInst jump;

	public ASMMlookup(MemoPoint m, PAsmInst next, PAsmInst skip) {
		super(next);
		this.memoPoint = m.id;
		this.jump = skip;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		switch (lookupMemo1(px, this.memoPoint)) {
		case PAsmAPI.NotFound:
			return this.next;
		case PAsmAPI.SuccFound:
			return this.jump;
		default:
			return raiseFail(px);
		}
	}
}
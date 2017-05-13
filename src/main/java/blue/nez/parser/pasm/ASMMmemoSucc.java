package blue.nez.parser.pasm;

import blue.nez.parser.ParserGrammar.MemoPoint;

public final class ASMMmemoSucc extends PAsmInst {
	public final int memoPoint;

	public ASMMmemoSucc(MemoPoint m, PAsmInst next) {
		super(next);
		this.memoPoint = m.id;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		storeMemo(px, this.memoPoint, popFail(px), true);
		return this.next;
	}
}
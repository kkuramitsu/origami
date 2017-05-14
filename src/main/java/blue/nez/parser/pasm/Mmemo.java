package blue.nez.parser.pasm;

import blue.nez.parser.ParserGrammar.MemoPoint;

public final class Mmemo extends PAsmInst {
	public final int memoPoint;

	public Mmemo(MemoPoint m, PAsmInst next) {
		super(next);
		this.memoPoint = m.id;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		storeMemo(px, this.memoPoint, popFail(px), true);
		return this.next;
	}
}
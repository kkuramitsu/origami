package blue.nez.parser.pasm;

import blue.nez.parser.ParserGrammar.MemoPoint;

public final class Mmemof extends PAsmInst {
	public final int memoPoint;

	public Mmemof(MemoPoint m) {
		super(null);
		this.memoPoint = m.id;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		storeMemo(px, this.memoPoint, px.pos, false);
		return raiseFail(px);
	}

}
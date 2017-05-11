package blue.nez.parser.pasm;

import blue.nez.parser.ParserContext;
import blue.nez.parser.ParserGrammar.MemoPoint;
import blue.nez.parser.pasm.PegAsm.AbstMemo;
import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.PAsmInst;

public final class ASMMlookup extends AbstMemo {
	public ASMMlookup(MemoPoint m, PAsmInst next, PAsmInst skip) {
		super(m, m.isStateful(), next, skip);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitLookup(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
		switch (sc.lookupMemo(this.uid)) {
		case ParserContext.NotFound:
			return this.next;
		case ParserContext.SuccFound:
			return this.jump;
		default:
			return sc.raiseFail();
		}
	}
}
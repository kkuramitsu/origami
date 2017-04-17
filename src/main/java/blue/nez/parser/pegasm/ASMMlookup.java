package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserContext;
import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.ParserCode.MemoPoint;
import blue.nez.parser.pegasm.PegAsm.AbstMemo;

public final class ASMMlookup extends AbstMemo {
	public ASMMlookup(MemoPoint m, PegAsmInst next, PegAsmInst skip) {
		super(m, m.isStateful(), next, skip);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitLookup(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
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